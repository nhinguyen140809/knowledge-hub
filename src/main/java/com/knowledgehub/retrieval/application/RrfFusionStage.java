package com.knowledgehub.retrieval.application;

import com.knowledgehub.retrieval.domain.FusionStrategy;
import com.knowledgehub.retrieval.domain.RankedList;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.config.AppProperties.Retrieval.HybridWeights;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The fan-in: merges the semantic, keyword and graph hit lists into one ranking with Reciprocal
 * Rank Fusion, each path weighted from configuration. After this stage the three paths become a
 * single ordered candidate list.
 */
@Component
class RrfFusionStage implements Stage<RetrievalContext> {

  private final FusionStrategy fusion;
  private final HybridWeights weights;
  private final int rrfK;

  RrfFusionStage(FusionStrategy fusion, AppProperties properties) {
    this.fusion = fusion;
    this.weights = properties.retrieval().weights();
    this.rrfK = properties.retrieval().rrfK();
  }

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    List<RankedList> lists =
        List.of(
            new RankedList("semantic", weights.vector(), context.semanticHits()),
            new RankedList("keyword", weights.keyword(), context.keywordHits()),
            new RankedList("graph", weights.graph(), context.graphHits()));
    context.setFusedHits(fusion.fuse(lists, rrfK));
    return context;
  }
}
