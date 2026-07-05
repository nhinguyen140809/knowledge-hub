package com.knowledgehub.retrieval.infrastructure.search;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Read side of the graph used to finish a query. Metadata is loaded for all node kinds in one
 * query: a {@code :Chunk} carries its own path/lines/ref, a {@code :CodeEntity} borrows the path
 * from its declaring {@code :File}, and a {@code :Commit} has no path — its identity is its hash,
 * returned as {@code commit_sha} with type {@code commit}. Both the load and the ref check honour
 * {@code allowedSources}, so assembly cannot leak a disallowed source either.
 */
@Component
class Neo4jRetrievalReader implements RetrievalReadPort {

  private static final String LOAD =
      """
      UNWIND $ids AS id
      OPTIONAL MATCH (c:Chunk {chunk_id: id})
      OPTIONAL MATCH (e:CodeEntity {entity_id: id})
      OPTIONAL MATCH (ef:File {file_id: e.file_id})
      OPTIONAL MATCH (m:Commit {commit_id: id})
      WITH id, c, e, ef, m, coalesce(c.source_id, e.source_id, m.source_id) AS source_id
      WHERE (c IS NOT NULL OR e IS NOT NULL OR m IS NOT NULL)
        AND ($unrestricted OR source_id IN $allowedSources)
      RETURN id,
        CASE
          WHEN c IS NOT NULL THEN 'chunk'
          WHEN e IS NOT NULL THEN 'entity'
          ELSE 'commit'
        END AS kind,
        source_id AS source_id,
        coalesce(c.path, ef.path) AS path,
        coalesce(c.line_start, e.line_start) AS line_start,
        coalesce(c.line_end, e.line_end) AS line_end,
        CASE WHEN m IS NOT NULL THEN 'commit' ELSE coalesce(c.type, 'code') END AS type,
        coalesce(c.ref, m.ref) AS ref,
        coalesce(c.indexed_at, m.indexed_at) AS indexed_at,
        coalesce(c.commit_sha, m.sha) AS commit_sha
      """;

  private static final String REF_INDEXED =
      """
      OPTIONAL MATCH (c:Chunk {ref: $ref})
      WHERE $unrestricted OR c.source_id IN $allowedSources
      RETURN count(c) > 0 AS indexed
      """;

  private final Neo4jClient client;

  Neo4jRetrievalReader(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public Map<String, HitMetadata> loadMetadata(Collection<String> ids, Filter filter) {
    if (ids.isEmpty() || SourceFilters.restrictedToNothing(filter)) {
      return Map.of();
    }
    return client
        .query(LOAD)
        .bindAll(
            Map.of(
                "ids", List.copyOf(ids),
                "unrestricted", filter.isUnrestricted(),
                "allowedSources", SourceFilters.allowedSources(filter)))
        .fetchAs(Loaded.class)
        .mappedBy(
            (t, row) ->
                new Loaded(
                    row.get("id").asString(),
                    new HitMetadata(
                        row.get("kind").asString(),
                        row.get("source_id").asString(),
                        stringOrNull(row.get("path")),
                        intOrNull(row.get("line_start")),
                        intOrNull(row.get("line_end")),
                        stringOrNull(row.get("type")),
                        stringOrNull(row.get("ref")),
                        stringOrNull(row.get("indexed_at")),
                        stringOrNull(row.get("commit_sha")),
                        List.of())))
        .all()
        .stream()
        .collect(Collectors.toMap(Loaded::id, Loaded::metadata, (a, b) -> a));
  }

  @Override
  public boolean refIndexed(String ref, Filter filter) {
    if (ref == null || SourceFilters.restrictedToNothing(filter)) {
      return false;
    }
    return client
        .query(REF_INDEXED)
        .bindAll(
            Map.of(
                "ref", ref,
                "unrestricted", filter.isUnrestricted(),
                "allowedSources", SourceFilters.allowedSources(filter)))
        .fetchAs(Boolean.class)
        .mappedBy((t, row) -> row.get("indexed").asBoolean())
        .one()
        .orElse(false);
  }

  private static String stringOrNull(Value value) {
    return value == null || value.isNull() ? null : value.asString();
  }

  private static Integer intOrNull(Value value) {
    return value == null || value.isNull() ? null : (int) value.asLong();
  }

  private record Loaded(String id, HitMetadata metadata) {}
}
