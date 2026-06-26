package com.knowledgehub.knowledge.infrastructure.persistence;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.HybridVectorStore;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.SparseVector;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Scale-out {@link com.knowledgehub.knowledge.domain.VectorStorePort}: vectors live in Qdrant while
 * the graph stays in Neo4j, linked by {@code chunk_id} (adds a dual-write + eviction sync concern).
 * Implements {@link HybridVectorStore} to expose Qdrant's native dense+sparse hybrid as a
 * capability.
 *
 * <p>Skeleton only — active when {@code app.vectorstore.mode=neo4j+qdrant}; the Qdrant client
 * wiring is completed in the scale-out phase.
 */
@Component
@ConditionalOnProperty(name = "app.vectorstore.mode", havingValue = "neo4j+qdrant")
public class QdrantVectorAdapter implements HybridVectorStore {

  @Override
  public void upsert(List<ChunkVector> chunks) {
    throw new UnsupportedOperationException("Qdrant adapter is not implemented yet");
  }

  @Override
  public List<ScoredId> search(float[] query, int k, Filter filter) {
    throw new UnsupportedOperationException("Qdrant adapter is not implemented yet");
  }

  @Override
  public List<ScoredId> hybridSearch(float[] dense, SparseVector sparse, int k, Filter filter) {
    throw new UnsupportedOperationException("Qdrant adapter is not implemented yet");
  }

  @Override
  public void deleteByChunkIds(List<String> chunkIds) {
    throw new UnsupportedOperationException("Qdrant adapter is not implemented yet");
  }

  @Override
  public void deleteBySource(String sourceId) {
    throw new UnsupportedOperationException("Qdrant adapter is not implemented yet");
  }
}
