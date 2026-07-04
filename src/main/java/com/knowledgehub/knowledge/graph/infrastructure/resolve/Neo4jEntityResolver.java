package com.knowledgehub.knowledge.graph.infrastructure.resolve;

import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link EntityResolver}. Looks entities up by their stored {@code qualified_name},
 * {@code name}, or declaring file {@code path}, always preferring a match in the requesting source
 * and widening to other sources so references can cross source boundaries. Each call resolves all
 * the given names in a single {@code UNWIND} query.
 *
 * <p>Exact resolution is conservative: it returns a match only when it is unambiguous (one in the
 * requesting source, or a single one anywhere), never guessing between collisions - a wrong
 * structural edge is worse than a missing one.
 */
@Component
class Neo4jEntityResolver implements EntityResolver {

  private static final String BY_QUALIFIED_NAME =
      "UNWIND $values AS value MATCH (e:CodeEntity {qualified_name: value})"
          + " RETURN value AS key, e.entity_id AS id, e.source_id AS src";

  private static final String BY_NAME =
      "UNWIND $values AS value MATCH (e:CodeEntity {name: value})"
          + " RETURN value AS key, e.entity_id AS id, e.source_id AS src";

  private static final String BY_PATH =
      "UNWIND $values AS value MATCH (:File {path: value})-[:DECLARES]->(e:CodeEntity)"
          + " RETURN value AS key, e.entity_id AS id, e.source_id AS src";

  private final Neo4jClient client;

  Neo4jEntityResolver(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public Map<String, String> resolve(Collection<String> qualifiedNames, ResolutionScope scope) {
    Map<String, String> resolved = new HashMap<>();
    grouped(BY_QUALIFIED_NAME, qualifiedNames)
        .forEach((key, hits) -> unambiguous(hits, scope).ifPresent(id -> resolved.put(key, id)));
    return resolved;
  }

  @Override
  public Map<String, List<String>> findByName(
      Collection<String> simpleNames, ResolutionScope scope) {
    return sourcePreferred(grouped(BY_NAME, simpleNames), scope);
  }

  @Override
  public Map<String, List<String>> findByPath(Collection<String> paths, ResolutionScope scope) {
    return sourcePreferred(grouped(BY_PATH, paths), scope);
  }

  /** Runs one batched lookup and groups the hits by the name/path they matched. */
  private Map<String, List<Hit>> grouped(String cypher, Collection<String> values) {
    if (values.isEmpty()) {
      return Map.of();
    }
    return client
        .query(cypher)
        .bind(List.copyOf(values))
        .to("values")
        .fetchAs(Hit.class)
        .mappedBy(
            (t, r) ->
                new Hit(r.get("key").asString(), r.get("id").asString(), r.get("src").asString()))
        .all()
        .stream()
        .collect(Collectors.groupingBy(Hit::key));
  }

  /** The single entity a qualified name points at, or empty when unresolved or ambiguous. */
  private static Optional<String> unambiguous(List<Hit> hits, ResolutionScope scope) {
    List<Hit> local = hits.stream().filter(h -> h.src().equals(scope.sourceId())).toList();
    if (local.size() == 1) {
      return Optional.of(local.get(0).id());
    }
    if (local.isEmpty() && hits.size() == 1) {
      return Optional.of(hits.get(0).id());
    }
    return Optional.empty();
  }

  /** Same-source matches first, then the rest; keeps only the entity ids. */
  private static Map<String, List<String>> sourcePreferred(
      Map<String, List<Hit>> grouped, ResolutionScope scope) {
    Map<String, List<String>> out = new HashMap<>();
    grouped.forEach(
        (key, hits) ->
            out.put(
                key,
                hits.stream()
                    .sorted(Comparator.comparing(h -> h.src().equals(scope.sourceId()) ? 0 : 1))
                    .map(Hit::id)
                    .toList()));
    return out;
  }

  private record Hit(String key, String id, String src) {}
}
