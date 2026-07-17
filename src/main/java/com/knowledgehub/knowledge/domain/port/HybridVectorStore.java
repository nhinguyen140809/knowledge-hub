package com.knowledgehub.knowledge.domain.port;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.SparseVector;
import java.util.List;

/**
 * Optional capability interface for vector stores that support native dense+sparse hybrid search.
 * The application opts in via {@code instanceof} and falls back to application-side RRF fusion
 * otherwise, so behaviour stays identical across adapters by default (reproducibility). Use
 * sparingly.
 */
public interface HybridVectorStore extends VectorStorePort {

  /**
   * Single-call hybrid search combining a dense and a sparse query vector.
   *
   * @param dense the dense query embedding
   * @param sparse the sparse query vector
   * @param k maximum number of results
   * @param filter source/ref/type restrictions applied as a pre-filter
   */
  List<ScoredId> hybridSearch(float[] dense, SparseVector sparse, int k, Filter filter);
}
