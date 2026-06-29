package com.knowledgehub.knowledge.indexing.domain;

/**
 * Tunables that shape chunking, passed to every {@link Chunker}. Built from configuration at the
 * application boundary so the chunkers stay free of framework concerns.
 *
 * @param maxTokens target maximum tokens per chunk (a chunk may be smaller, never deliberately
 *     larger except an indivisible unit such as a single oversized function)
 * @param overlap token overlap carried between adjacent chunks of the same unit
 */
public record ChunkConfig(int maxTokens, int overlap) {

  public ChunkConfig {
    if (maxTokens <= 0) {
      throw new IllegalArgumentException("maxTokens must be positive");
    }
    if (overlap < 0) {
      throw new IllegalArgumentException("overlap must not be negative");
    }
    if (overlap >= maxTokens) {
      throw new IllegalArgumentException("overlap must be smaller than maxTokens");
    }
  }
}
