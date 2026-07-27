package com.knowledgehub.system.infrastructure.persistence;

import com.knowledgehub.system.domain.port.VectorStoreMetrics;
import io.qdrant.client.QdrantClient;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reports the Qdrant collection's point count — the {@code vectors} figure in knowledge stats. */
@Component
class QdrantVectorStoreMetricsAdapter implements VectorStoreMetrics {

  private final QdrantClient client;
  private final String collectionName;

  QdrantVectorStoreMetricsAdapter(
      QdrantClient client,
      @Value("${spring.ai.vectorstore.qdrant.collection-name:knowledge-embeddings}")
          String collectionName) {
    this.client = client;
    this.collectionName = collectionName;
  }

  @Override
  public long pointCount() {
    return await(client.countAsync(collectionName));
  }

  private static <T> T await(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while counting Qdrant points", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to count Qdrant points", e);
    }
  }
}
