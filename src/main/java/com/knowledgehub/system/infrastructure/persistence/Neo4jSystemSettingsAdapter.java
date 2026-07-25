package com.knowledgehub.system.infrastructure.persistence;

import com.knowledgehub.system.domain.port.SystemSettings;
import java.util.Optional;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Persists system settings as a single {@code :SystemSettings} node, keyed by a fixed id so there
 * is only ever one. Writes are an idempotent upsert.
 */
@Component
class Neo4jSystemSettingsAdapter implements SystemSettings {

  private static final String READ =
      "MATCH (s:SystemSettings {id: 'singleton'}) RETURN s.product_name AS productName";

  private static final String WRITE =
      "MERGE (s:SystemSettings {id: 'singleton'}) SET s.product_name = $productName";

  private final Neo4jClient client;

  Neo4jSystemSettingsAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public Optional<String> productName() {
    return client
        .query(READ)
        .fetch()
        .one()
        .map(row -> (String) row.get("productName"))
        .filter(name -> !name.isBlank());
  }

  @Override
  public void setProductName(String productName) {
    client.query(WRITE).bind(productName).to("productName").run();
  }
}
