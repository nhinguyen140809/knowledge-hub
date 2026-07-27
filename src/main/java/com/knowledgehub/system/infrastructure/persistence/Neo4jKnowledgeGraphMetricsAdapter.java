package com.knowledgehub.system.infrastructure.persistence;

import com.knowledgehub.system.domain.GraphMetrics;
import com.knowledgehub.system.domain.port.KnowledgeGraphMetrics;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link KnowledgeGraphMetrics}. Three independent counting queries: a fresh top-level
 * aggregation always returns exactly one row (0 when nothing matches), which a chained multi-stage
 * query would not — a plain {@code MATCH} that finds nothing drops every row already flowing
 * through it.
 */
@Component
class Neo4jKnowledgeGraphMetricsAdapter implements KnowledgeGraphMetrics {

  private static final String COUNT_DOCUMENTS = "MATCH (c:Chunk) RETURN count(c) AS total";

  private static final String COUNT_NODES =
      "MATCH (n) WHERE n:Source OR n:File OR n:Chunk OR n:CodeEntity OR n:Commit"
          + " RETURN count(n) AS total";

  private static final String COUNT_EDGES =
      "MATCH (a)-[r]->(b)"
          + " WHERE (a:Source OR a:File OR a:Chunk OR a:CodeEntity OR a:Commit)"
          + " AND (b:Source OR b:File OR b:Chunk OR b:CodeEntity OR b:Commit)"
          + " RETURN count(r) AS total";

  private final Neo4jClient client;

  Neo4jKnowledgeGraphMetricsAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public GraphMetrics snapshot() {
    return new GraphMetrics(count(COUNT_DOCUMENTS), count(COUNT_NODES), count(COUNT_EDGES));
  }

  private long count(String query) {
    return client
        .query(query)
        .fetchAs(Long.class)
        .mappedBy((t, row) -> row.get("total").asLong())
        .one()
        .orElse(0L);
  }
}
