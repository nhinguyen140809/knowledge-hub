package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.graph.application.LinkSummary;
import com.knowledgehub.knowledge.graph.application.LinkingService;
import com.knowledgehub.shared.pipeline.Stage;
import org.springframework.stereotype.Component;

/**
 * Final filter: links the artifact's stored nodes into the knowledge graph (structural relations
 * plus accepted cross-artifact links). It runs after the nodes are stored, so the relationship
 * targets already exist, and delegates the actual linking to the graph layer's {@link
 * LinkingService} — the indexing pipeline only supplies the artifact and its chunks.
 */
@Component
class LinkStage implements Stage<IndexingContext> {

  private final LinkingService linkingService;

  LinkStage(LinkingService linkingService) {
    this.linkingService = linkingService;
  }

  @Override
  public IndexingContext apply(IndexingContext context) {
    if (context.isSkipped()) {
      return context;
    }
    LinkSummary summary = linkingService.link(context.artifact(), context.chunks());
    context.setRelationshipsLinked(summary.relationshipsWritten());
    return context;
  }
}
