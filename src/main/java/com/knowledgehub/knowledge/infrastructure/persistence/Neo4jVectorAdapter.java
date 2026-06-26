package com.knowledgehub.knowledge.infrastructure.persistence;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Default {@link VectorStorePort}: stores embeddings on {@code :Chunk(embedding)} and searches via
 * the {@code chunk_embedding} vector index, all through plain Cypher. Active in {@code neo4j} mode
 * (the default); {@code neo4j+qdrant} mode swaps in the Qdrant adapter instead.
 *
 * <p>The ACL pre-filter is pushed into the query: when the {@link Filter} restricts sources, the
 * {@code WHERE} runs inside the same Cypher so disallowed nodes are never returned (FR-8.6).
 */
@Component
@ConditionalOnProperty(name = "app.vectorstore.mode", havingValue = "neo4j", matchIfMissing = true)
public class Neo4jVectorAdapter implements VectorStorePort {

  private static final String INDEX = "chunk_embedding";

  private final Neo4jClient neo4jClient;

  public Neo4jVectorAdapter(Neo4jClient neo4jClient) {
    this.neo4jClient = neo4jClient;
  }

  @Override
  public void upsert(List<ChunkVector> chunks) {
    if (chunks.isEmpty()) {
      return;
    }
    List<Map<String, Object>> rows = new ArrayList<>(chunks.size());
    for (ChunkVector chunk : chunks) {
      rows.add(
          Map.of(
              "id", chunk.chunkId(),
              "metadata", chunk.metadata(),
              "embedding", toDoubleList(chunk.embedding())));
    }
    neo4jClient
        .query(
            "UNWIND $rows AS row"
                + " MERGE (c:Chunk {chunk_id: row.id})"
                + " SET c += row.metadata"
                + " SET c.embedding = row.embedding")
        .bind(rows)
        .to("rows")
        .run();
  }

  @Override
  public List<ScoredId> search(float[] query, int k, Filter filter) {
    String cypher =
        "CALL db.index.vector.queryNodes($index, $k, $query) YIELD node, score"
            + " WITH node, score"
            + " WHERE $unrestricted OR node.source_id IN $sources"
            + " RETURN node.chunk_id AS id, score ORDER BY score DESC";
    return neo4jClient
        .query(cypher)
        .bindAll(
            Map.of(
                "index",
                INDEX,
                "k",
                k,
                "query",
                toDoubleList(query),
                "unrestricted",
                filter.isUnrestricted(),
                "sources",
                filter.isUnrestricted() ? List.of() : List.copyOf(filter.allowedSources())))
        .fetchAs(ScoredId.class)
        .mappedBy((t, r) -> new ScoredId(r.get("id").asString(), r.get("score").asDouble()))
        .all()
        .stream()
        .toList();
  }

  @Override
  public void deleteByChunkIds(List<String> chunkIds) {
    if (chunkIds.isEmpty()) {
      return;
    }
    neo4jClient
        .query("MATCH (c:Chunk) WHERE c.chunk_id IN $ids DETACH DELETE c")
        .bind(chunkIds)
        .to("ids")
        .run();
  }

  @Override
  public void deleteBySource(String sourceId) {
    neo4jClient
        .query("MATCH (c:Chunk {source_id: $sourceId}) DETACH DELETE c")
        .bind(sourceId)
        .to("sourceId")
        .run();
  }

  private static List<Double> toDoubleList(float[] vector) {
    List<Double> out = new ArrayList<>(vector.length);
    for (float value : vector) {
      out.add((double) value);
    }
    return out;
  }
}
