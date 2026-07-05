package com.knowledgehub.knowledge.indexing.infrastructure.persistence;

import com.knowledgehub.knowledge.indexing.domain.CommitRepository;
import com.knowledgehub.knowledge.ingestion.domain.CommitRecord;
import com.knowledgehub.shared.id.IdFactory;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link CommitRepository}. Writes {@code :Commit} nodes keyed by {@code commit_id}
 * and a {@code MODIFIES} edge to every {@code :File} node its diff touched. The edge target is
 * matched, never created: a path with no indexed file gets no edge. {@code MODIFIES} is read
 * straight from the commit's diff, so it is stored with confidence 1 — unlike the heuristic
 * cross-artifact links, there is nothing to guess.
 */
@Component
class Neo4jCommitAdapter implements CommitRepository {

  private static final String EXISTING_SHAS =
      "MATCH (m:Commit {source_id: $sourceId}) WHERE m.sha IN $shas RETURN m.sha AS sha";

  private static final String UPSERT_NODES =
      """
      UNWIND $rows AS row
      MERGE (m:Commit {commit_id: row.commit_id})
        SET m.source_id = $sourceId, m.sha = row.sha, m.message = row.message,
            m.author = row.author, m.authored_at = row.authored_at,
            m.ref = $ref, m.indexed_at = $indexedAt
      """;

  private static final String LINK_MODIFIED_FILES =
      """
      UNWIND $rows AS row
      MATCH (m:Commit {commit_id: row.commit_id})
      UNWIND row.paths AS path
      MATCH (f:File {source_id: $sourceId, path: path})
      MERGE (m)-[r:MODIFIES]->(f)
        SET r.confidence = 1.0
      """;

  private final Neo4jClient client;

  Neo4jCommitAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public Set<String> existingShas(String sourceId, Collection<String> shas) {
    if (shas.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        client
            .query(EXISTING_SHAS)
            .bind(sourceId)
            .to("sourceId")
            .bind(List.copyOf(shas))
            .to("shas")
            .fetchAs(String.class)
            .all());
  }

  @Override
  public void upsertAll(
      String sourceId, String ref, Instant indexedAt, List<CommitRecord> commits) {
    if (commits.isEmpty()) {
      return;
    }
    List<Map<String, Object>> rows =
        commits.stream().map(commit -> toRow(sourceId, commit)).toList();
    Map<String, Object> parameters =
        new HashMap<>(
            Map.of("rows", rows, "sourceId", sourceId, "indexedAt", indexedAt.toString()));
    parameters.put("ref", ref);
    client.query(UPSERT_NODES).bindAll(parameters).run();
    client.query(LINK_MODIFIED_FILES).bindAll(parameters).run();
  }

  private static Map<String, Object> toRow(String sourceId, CommitRecord commit) {
    Map<String, Object> row = new HashMap<>();
    row.put("commit_id", IdFactory.commitId(sourceId, commit.sha()));
    row.put("sha", commit.sha());
    row.put("message", commit.message());
    row.put("author", commit.author());
    row.put("authored_at", commit.authoredAt().toString());
    row.put("paths", commit.changedPaths());
    return row;
  }
}
