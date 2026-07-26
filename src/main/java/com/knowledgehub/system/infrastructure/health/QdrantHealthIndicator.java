package com.knowledgehub.system.infrastructure.health;

import io.qdrant.client.QdrantClient;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reachability of Qdrant: a cheap collection-info call, no embedding cost. Registered under the
 * name {@code qdrant} (the explicit bean name), read back by {@code DependencyHealthService}.
 */
@Component("qdrant")
public class QdrantHealthIndicator implements HealthIndicator {

  private final QdrantClient client;
  private final String collectionName;

  public QdrantHealthIndicator(
      QdrantClient client,
      @Value("${spring.ai.vectorstore.qdrant.collection-name:knowledge-embeddings}")
          String collectionName) {
    this.client = client;
    this.collectionName = collectionName;
  }

  @Override
  public Health health() {
    try {
      client.getCollectionInfoAsync(collectionName).get();
      return Health.up().build();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Health.down(e).build();
    } catch (ExecutionException e) {
      return Health.down(e).build();
    }
  }
}
