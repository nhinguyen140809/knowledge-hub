package com.knowledgehub.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level toggles bound from the {@code app.*} configuration namespace. Centralizes
 * config-as-a-feature switches so they are typed and validated instead of read ad-hoc via
 * {@code @Value}.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(VectorStore vectorstore) {

  public AppProperties {
    if (vectorstore == null) {
      vectorstore = new VectorStore(null);
    }
  }

  /**
   * Vector store wiring.
   *
   * @param mode {@code neo4j} (default) or {@code neo4j+qdrant} when scaling the vector index out
   *     to a dedicated engine.
   */
  public record VectorStore(String mode) {
    public VectorStore {
      if (mode == null || mode.isBlank()) {
        mode = "neo4j";
      }
    }
  }
}
