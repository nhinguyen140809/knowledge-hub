package com.knowledgehub.knowledge.sync.infrastructure.persistence;

import com.knowledgehub.knowledge.ingestion.application.port.SourceFreshness;
import com.knowledgehub.knowledge.sync.domain.FreshnessInfo;
import com.knowledgehub.knowledge.sync.domain.port.FreshnessRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Stores each source's last-indexed state as a single {@code :SourceFreshness} node, keyed by
 * {@code source_id}. The differ reads {@code commit_sha} to know where the next sync starts, and
 * the status endpoint reads {@code indexed_at} to report how fresh the index is. Writes are an
 * idempotent upsert. Also serves ingestion's {@link SourceFreshness} read port, so the source views
 * can report {@code updatedAt} from the same node without ingestion reaching into sync.
 */
@Component
class Neo4jFreshnessAdapter implements FreshnessRepository, SourceFreshness {

  private static final String FIND =
      "MATCH (s:SourceFreshness {source_id: $sourceId})"
          + " RETURN s.indexed_at AS indexedAt, s.commit_sha AS commitSha, s.ref AS ref";

  private static final String LAST_INDEXED =
      "MATCH (s:SourceFreshness) WHERE s.source_id IN $ids"
          + " RETURN s.source_id AS sourceId, s.indexed_at AS indexedAt";

  private static final String SAVE =
      "MERGE (s:SourceFreshness {source_id: $sourceId})"
          + " SET s.indexed_at = $indexedAt, s.commit_sha = $commitSha, s.ref = $ref";

  private static final String DELETE = "MATCH (s:SourceFreshness {source_id: $sourceId}) DELETE s";

  private final Neo4jClient client;

  Neo4jFreshnessAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public Optional<FreshnessInfo> find(String sourceId) {
    return client
        .query(FIND)
        .bind(sourceId)
        .to("sourceId")
        .fetchAs(FreshnessInfo.class)
        .mappedBy(
            (t, row) ->
                new FreshnessInfo(
                    sourceId,
                    Instant.parse(row.get("indexedAt").asString()),
                    stringOrNull(row.get("commitSha")),
                    stringOrNull(row.get("ref"))))
        .one();
  }

  @Override
  public Map<String, Instant> lastIndexedAt(Collection<String> sourceIds) {
    if (sourceIds.isEmpty()) {
      return Map.of();
    }
    Map<String, Instant> byId = new HashMap<>();
    client
        .query(LAST_INDEXED)
        .bind(List.copyOf(sourceIds))
        .to("ids")
        .fetch()
        .all()
        .forEach(
            row ->
                byId.put(
                    (String) row.get("sourceId"), Instant.parse((String) row.get("indexedAt"))));
    return byId;
  }

  @Override
  public void save(FreshnessInfo freshness) {
    Map<String, Object> params = new HashMap<>();
    params.put("sourceId", freshness.sourceId());
    params.put("indexedAt", freshness.indexedAt().toString());
    params.put("commitSha", freshness.commitSha());
    params.put("ref", freshness.ref());
    client.query(SAVE).bindAll(params).run();
  }

  @Override
  public void deleteBySource(String sourceId) {
    client.query(DELETE).bind(sourceId).to("sourceId").run();
  }

  private static String stringOrNull(Value value) {
    return value == null || value.isNull() ? null : value.asString();
  }
}
