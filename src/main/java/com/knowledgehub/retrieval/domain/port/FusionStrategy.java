package com.knowledgehub.retrieval.domain.port;

import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.RankedList;
import java.util.List;

/**
 * Merges the ranked outputs of the retrieval paths into one ranking. The default is Reciprocal Rank
 * Fusion, which combines by rank position rather than raw score, so paths with incomparable score
 * scales fuse fairly and the result is reproducible for a given set of inputs.
 */
public interface FusionStrategy {

  /**
   * Fuses the given ranked lists into a single ranking, best-first.
   *
   * @param lists each path's weighted, rank-ordered output
   * @param rrfK the fusion constant {@code k}; larger flattens the contribution of rank
   * @return one ranking with a fused score per id, highest first
   */
  List<ScoredId> fuse(List<RankedList> lists, int rrfK);
}
