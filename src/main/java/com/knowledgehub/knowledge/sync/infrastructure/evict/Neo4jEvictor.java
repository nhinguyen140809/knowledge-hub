package com.knowledgehub.knowledge.sync.infrastructure.evict;

import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.sync.domain.Evictor;
import java.util.Collection;
import java.util.List;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Evicts stale knowledge from both stores together. It first reads the {@code chunk_id}s to remove
 * so it can drop their vectors from Qdrant, then deletes the matching nodes and edges from Neo4j.
 * Removing the vectors first means a query can never hit a vector whose node is already gone.
 */
@Component
class Neo4jEvictor implements Evictor {

  private static final String CHUNK_IDS_FOR_FILES =
      "MATCH (f:File {source_id: $sourceId})<-[:PART_OF]-(c:Chunk)"
          + " WHERE f.path IN $paths RETURN c.chunk_id AS id";

  private static final String DELETE_FILES =
      """
      MATCH (f:File {source_id: $sourceId}) WHERE f.path IN $paths
      OPTIONAL MATCH (f)<-[:PART_OF]-(c:Chunk)
      OPTIONAL MATCH (f)-[:DECLARES]->(e:CodeEntity)
      OPTIONAL MATCH (e)-[:CONTAINS*]->(child:CodeEntity)
      DETACH DELETE c, child, e, f
      """;

  private static final String STALE_CHUNK_IDS =
      "MATCH (f:File {source_id: $sourceId, path: $path})<-[:PART_OF]-(c:Chunk)"
          + " WHERE NOT c.chunk_id IN $keep RETURN c.chunk_id AS id";

  private static final String DELETE_CHUNKS_BY_IDS =
      "UNWIND $ids AS id MATCH (c:Chunk {chunk_id: id}) DETACH DELETE c";

  private static final String DELETE_SOURCE =
      "MATCH (n) WHERE n.source_id = $sourceId AND (n:File OR n:Chunk OR n:CodeEntity)"
          + " DETACH DELETE n";

  private final Neo4jClient client;
  private final VectorStorePort vectorStore;

  Neo4jEvictor(Neo4jClient client, VectorStorePort vectorStore) {
    this.client = client;
    this.vectorStore = vectorStore;
  }

  @Override
  public void evictFiles(String sourceId, Collection<String> paths) {
    if (paths.isEmpty()) {
      return;
    }
    List<String> pathList = List.copyOf(paths);
    List<String> chunkIds =
        client
            .query(CHUNK_IDS_FOR_FILES)
            .bind(sourceId)
            .to("sourceId")
            .bind(pathList)
            .to("paths")
            .fetchAs(String.class)
            .mappedBy((t, row) -> row.get("id").asString())
            .all()
            .stream()
            .toList();
    vectorStore.deleteByChunkIds(chunkIds);
    client.query(DELETE_FILES).bind(sourceId).to("sourceId").bind(pathList).to("paths").run();
  }

  @Override
  public void retainChunks(String sourceId, String path, Collection<String> keepChunkIds) {
    List<String> staleIds =
        client
            .query(STALE_CHUNK_IDS)
            .bind(sourceId)
            .to("sourceId")
            .bind(path)
            .to("path")
            .bind(List.copyOf(keepChunkIds))
            .to("keep")
            .fetchAs(String.class)
            .mappedBy((t, row) -> row.get("id").asString())
            .all()
            .stream()
            .toList();
    if (staleIds.isEmpty()) {
      return;
    }
    vectorStore.deleteByChunkIds(staleIds);
    client.query(DELETE_CHUNKS_BY_IDS).bind(staleIds).to("ids").run();
  }

  @Override
  public void evictSource(String sourceId) {
    vectorStore.deleteBySource(sourceId);
    client.query(DELETE_SOURCE).bind(sourceId).to("sourceId").run();
  }
}
