package com.knowledgehub.system.application;

import com.knowledgehub.system.domain.GraphMetrics;
import com.knowledgehub.system.domain.KnowledgeStats;
import com.knowledgehub.system.domain.port.KnowledgeGraphMetrics;
import com.knowledgehub.system.domain.port.VectorStoreMetrics;
import org.springframework.stereotype.Service;

/** Aggregates how much has been indexed: graph scale plus the vector store's point count. */
@Service
public class KnowledgeStatsService {

  private final KnowledgeGraphMetrics graph;
  private final VectorStoreMetrics vectors;

  public KnowledgeStatsService(KnowledgeGraphMetrics graph, VectorStoreMetrics vectors) {
    this.graph = graph;
    this.vectors = vectors;
  }

  public KnowledgeStats currentStats() {
    GraphMetrics snapshot = graph.snapshot();
    return new KnowledgeStats(
        snapshot.documents(), snapshot.graphNodes(), snapshot.graphEdges(), vectors.pointCount());
  }
}
