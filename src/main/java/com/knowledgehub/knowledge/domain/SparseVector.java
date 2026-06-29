package com.knowledgehub.knowledge.domain;

/**
 * A sparse vector (parallel index/value arrays), used only by adapters that support native
 * dense+sparse hybrid search via {@link HybridVectorStore}.
 *
 * @param indices the non-zero dimension indices
 * @param values the value at each corresponding index
 */
public record SparseVector(int[] indices, float[] values) {}
