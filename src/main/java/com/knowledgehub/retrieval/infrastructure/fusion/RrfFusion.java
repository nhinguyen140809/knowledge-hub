package com.knowledgehub.retrieval.infrastructure.fusion;

import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.RankedList;
import com.knowledgehub.retrieval.domain.port.FusionStrategy;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Reciprocal Rank Fusion. Each list contributes {@code weight / (k + rank)} to every id it ranks
 * (rank 1-based), and the contributions sum across lists. Using rank rather than the raw score lets
 * paths with incomparable scales - cosine, BM25, graph distance - combine fairly.
 *
 * <p>The output is fully determined by the input lists and {@code k}: ties in the fused score break
 * on the id, so the same inputs always yield the same ranking (reproducibility).
 */
@Component
public class RrfFusion implements FusionStrategy {

  @Override
  public List<ScoredId> fuse(List<RankedList> lists, int rrfK) {
    Map<String, Double> fused = new HashMap<>();
    for (RankedList list : lists) {
      int rank = 1;
      for (ScoredId hit : list.ids()) {
        fused.merge(hit.chunkId(), list.weight() / (rrfK + rank), Double::sum);
        rank++;
      }
    }
    return fused.entrySet().stream()
        .sorted(
            Comparator.comparingDouble(Map.Entry<String, Double>::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey))
        .map(entry -> new ScoredId(entry.getKey(), entry.getValue()))
        .toList();
  }
}
