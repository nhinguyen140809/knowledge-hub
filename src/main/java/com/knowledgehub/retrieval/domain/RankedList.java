package com.knowledgehub.retrieval.domain;

import com.knowledgehub.knowledge.domain.ScoredId;
import java.util.List;

/**
 * One retrieval path's ranked output handed to fusion: the path's name (for logging and weighting),
 * its relative weight, and its results in rank order. Rank Fusion uses position, not the raw score,
 * so paths with incomparable score scales (cosine vs BM25 vs graph distance) combine fairly.
 *
 * @param source the path name (e.g. {@code semantic}, {@code keyword}, {@code graph})
 * @param weight the path's relative weight in fusion
 * @param ids the path's hits, best-first
 */
public record RankedList(String source, double weight, List<ScoredId> ids) {

  public RankedList {
    ids = ids == null ? List.of() : List.copyOf(ids);
  }
}
