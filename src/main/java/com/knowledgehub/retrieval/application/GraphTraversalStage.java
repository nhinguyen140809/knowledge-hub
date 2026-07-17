package com.knowledgehub.retrieval.application;

import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.port.GraphTraversalPort;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The graph retrieval path: expands the best semantic and keyword hits one hop along graph
 * relationships to pull in related context. Unlike the other two paths it depends on their results
 * for seeds, so it runs after them rather than alongside; fusion then combines all three.
 */
@Component
class GraphTraversalStage implements Stage<RetrievalContext> {

  /** Cap on how many of the best semantic/keyword hits seed the expansion, to bound traversal. */
  private static final int SEED_LIMIT = 25;

  private final GraphTraversalPort graphTraversal;
  private final int candidateK;

  GraphTraversalStage(GraphTraversalPort graphTraversal, AppProperties properties) {
    this.graphTraversal = graphTraversal;
    this.candidateK = properties.retrieval().candidateK();
  }

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    Set<String> seeds = new LinkedHashSet<>();
    collectSeeds(context.semanticHits(), seeds);
    collectSeeds(context.keywordHits(), seeds);
    if (seeds.isEmpty()) {
      return context;
    }
    context.setGraphHits(graphTraversal.expand(seeds, candidateK, context.aclFilter()));
    return context;
  }

  /** Adds hit ids until the seed cap is reached, preserving the best-first order. */
  private static void collectSeeds(List<ScoredId> hits, Set<String> seeds) {
    for (ScoredId hit : hits) {
      if (seeds.size() >= SEED_LIMIT) {
        return;
      }
      seeds.add(hit.chunkId());
    }
  }
}
