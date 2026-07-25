package com.knowledgehub.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Retrieval tunables, bound from {@code app.retrieval.*}.
 *
 * @param topK default number of results returned when the caller does not specify (default 10)
 * @param candidateK how many candidates each search path fetches before fusion (default 50)
 * @param rrfK the Reciprocal Rank Fusion constant {@code k} (default 60); larger flattens the
 *     contribution of rank
 * @param weights per-path fusion weights
 * @param cacheTtl how long a cached result stays fresh (default 60s)
 * @param cacheMaxSize maximum number of cached query results (default 1000)
 */
@Validated
@ConfigurationProperties("app.retrieval")
public record RetrievalProperties(
    @Positive Integer topK,
    @Positive Integer candidateK,
    @Positive Integer rrfK,
    @Valid HybridWeights weights,
    Duration cacheTtl,
    @Positive Integer cacheMaxSize) {
  public RetrievalProperties {
    if (topK == null) {
      topK = 10;
    }
    if (candidateK == null) {
      candidateK = 50;
    }
    if (rrfK == null) {
      rrfK = 60;
    }
    if (weights == null) {
      weights = new HybridWeights(null, null, null);
    }
    if (cacheTtl == null) {
      cacheTtl = Duration.ofSeconds(60);
    }
    if (cacheMaxSize == null) {
      cacheMaxSize = 1000;
    }
  }

  /**
   * Relative weight of each retrieval path in fusion. Semantic and keyword default to 1; graph
   * expansion is supporting context, so it defaults lower.
   *
   * @param vector weight of the semantic (vector) path (default 1.0)
   * @param keyword weight of the keyword (BM25) path (default 1.0)
   * @param graph weight of the graph-traversal path (default 0.5)
   */
  public record HybridWeights(
      @PositiveOrZero Double vector, @PositiveOrZero Double keyword, @PositiveOrZero Double graph) {
    public HybridWeights {
      if (vector == null) {
        vector = 1.0;
      }
      if (keyword == null) {
        keyword = 1.0;
      }
      if (graph == null) {
        graph = 0.5;
      }
    }
  }
}
