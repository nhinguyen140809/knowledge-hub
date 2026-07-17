package com.knowledgehub.knowledge.indexing.infrastructure.persistence;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.port.ChunkRepository;
import com.knowledgehub.knowledge.ingestion.domain.Provenance;
import com.knowledgehub.knowledge.ingestion.domain.VersionRef;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link ChunkRepository}. Writes {@code :Chunk} nodes with their provenance, linked
 * {@code PART_OF} a {@code :File} and (for code) {@code CHUNK_OF} a {@code :CodeEntity}. All writes
 * are idempotent {@code MERGE}s keyed by the content-derived ids, and all Cypher is parameterized.
 * The matching vector lives in the vector store, joined by {@code chunk_id}.
 */
@Component
class Neo4jChunkAdapter implements ChunkRepository {

  private static final String UPSERT =
      """
      UNWIND $rows AS row
      MERGE (f:File {file_id: row.file_id})
        SET f.source_id = row.source_id, f.path = row.path, f.content_hash = row.file_content_hash
      MERGE (c:Chunk {chunk_id: row.chunk_id})
        SET c.source_id = row.source_id, c.file_id = row.file_id, c.path = row.path,
            c.type = row.type, c.text = row.text, c.content_hash = row.content_hash,
            c.token_count = row.token_count, c.line_start = row.line_start,
            c.line_end = row.line_end, c.ref = row.ref, c.commit_sha = row.commit_sha,
            c.indexed_at = row.indexed_at
      MERGE (c)-[:PART_OF]->(f)
      FOREACH (_ IN CASE WHEN row.entity_id IS NULL THEN [] ELSE [1] END |
        MERGE (e:CodeEntity {entity_id: row.entity_id})
        MERGE (c)-[:CHUNK_OF]->(e))
      """;

  private static final String EXISTING_HASHES =
      "MATCH (c:Chunk {source_id: $sourceId}) WHERE c.content_hash IN $hashes"
          + " RETURN c.content_hash AS hash";

  private static final String DELETE_BY_IDS =
      "UNWIND $ids AS id MATCH (c:Chunk {chunk_id: id}) DETACH DELETE c";

  private static final String DELETE_BY_SOURCE =
      "MATCH (c:Chunk {source_id: $sourceId}) DETACH DELETE c";

  private final Neo4jClient client;

  Neo4jChunkAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public void upsertAll(List<Chunk> chunks) {
    if (chunks.isEmpty()) {
      return;
    }
    List<Map<String, Object>> rows = chunks.stream().map(Neo4jChunkAdapter::toRow).toList();
    client.query(UPSERT).bind(rows).to("rows").run();
  }

  @Override
  public Set<String> existingContentHashes(String sourceId, Collection<String> contentHashes) {
    if (contentHashes.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        client
            .query(EXISTING_HASHES)
            .bind(sourceId)
            .to("sourceId")
            .bind(List.copyOf(contentHashes))
            .to("hashes")
            .fetchAs(String.class)
            .all());
  }

  @Override
  public void deleteByChunkIds(List<String> chunkIds) {
    if (chunkIds.isEmpty()) {
      return;
    }
    client.query(DELETE_BY_IDS).bind(chunkIds).to("ids").run();
  }

  @Override
  public void deleteBySource(String sourceId) {
    client.query(DELETE_BY_SOURCE).bind(sourceId).to("sourceId").run();
  }

  private static Map<String, Object> toRow(Chunk chunk) {
    Provenance provenance = chunk.provenance();
    Optional<VersionRef> version = provenance.version();
    Map<String, Object> row = new HashMap<>();
    row.put("chunk_id", chunk.chunkId());
    row.put("source_id", chunk.sourceId());
    row.put("file_id", chunk.fileId());
    row.put("path", chunk.path());
    row.put("type", chunk.type().wireName());
    row.put("text", chunk.text());
    row.put("content_hash", chunk.contentHash());
    row.put("file_content_hash", provenance.contentHash());
    row.put("token_count", chunk.tokenCount());
    row.put("line_start", chunk.lineStart());
    row.put("line_end", chunk.lineEnd());
    row.put("entity_id", chunk.entityId());
    row.put("ref", version.map(VersionRef::ref).orElse(null));
    row.put("commit_sha", version.map(VersionRef::commitSha).orElse(null));
    row.put("indexed_at", provenance.indexedAt().toString());
    return row;
  }
}
