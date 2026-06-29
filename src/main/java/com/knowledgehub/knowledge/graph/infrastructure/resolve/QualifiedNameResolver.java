package com.knowledgehub.knowledge.graph.infrastructure.resolve;

import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link EntityResolver}. Looks entities up by their stored {@code qualified_name},
 * {@code name}, or declaring file {@code path}, always preferring a match in the requesting source
 * and widening to other sources so references can cross source boundaries.
 *
 * <p>Exact resolution is conservative: it returns a match only when it is unambiguous (one in the
 * requesting source, or a single one anywhere), never guessing between collisions — a wrong
 * structural edge is worse than a missing one.
 */
@Component
class QualifiedNameResolver implements EntityResolver {

  private static final String BY_QUALIFIED_NAME =
      "MATCH (e:CodeEntity {qualified_name: $value})"
          + " RETURN e.entity_id AS id, e.source_id AS src";

  private static final String BY_NAME =
      "MATCH (e:CodeEntity {name: $value}) RETURN e.entity_id AS id, e.source_id AS src";

  private static final String BY_PATH =
      "MATCH (:File {path: $value})-[:DECLARES]->(e:CodeEntity)"
          + " RETURN e.entity_id AS id, e.source_id AS src";

  private final Neo4jClient client;

  QualifiedNameResolver(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public Optional<String> resolve(String qualifiedName, ResolutionScope scope) {
    List<Match> matches = query(BY_QUALIFIED_NAME, qualifiedName);
    List<Match> local = matches.stream().filter(m -> m.src().equals(scope.sourceId())).toList();
    if (local.size() == 1) {
      return Optional.of(local.get(0).id());
    }
    if (local.isEmpty() && matches.size() == 1) {
      return Optional.of(matches.get(0).id());
    }
    return Optional.empty();
  }

  @Override
  public List<String> findByName(String simpleName, ResolutionScope scope) {
    return sourcePreferred(query(BY_NAME, simpleName), scope);
  }

  @Override
  public List<String> findByPath(String path, ResolutionScope scope) {
    return sourcePreferred(query(BY_PATH, path), scope);
  }

  private List<Match> query(String cypher, String value) {
    return client
        .query(cypher)
        .bind(value)
        .to("value")
        .fetchAs(Match.class)
        .mappedBy((t, r) -> new Match(r.get("id").asString(), r.get("src").asString()))
        .all()
        .stream()
        .toList();
  }

  /** Same-source matches first, then the rest; preserves the entity ids. */
  private static List<String> sourcePreferred(List<Match> matches, ResolutionScope scope) {
    return matches.stream()
        .sorted(Comparator.comparing(m -> m.src().equals(scope.sourceId()) ? 0 : 1))
        .map(Match::id)
        .toList();
  }

  private record Match(String id, String src) {}
}
