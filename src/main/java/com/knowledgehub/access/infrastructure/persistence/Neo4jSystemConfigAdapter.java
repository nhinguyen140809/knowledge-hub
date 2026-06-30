package com.knowledgehub.access.infrastructure.persistence;

import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.SystemConfigRepository;
import java.util.Map;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link SystemConfigRepository}. The default policy is a single {@code :SystemConfig}
 * node keyed by {@code default_policy}; absence means {@link DefaultPolicy#DENY} (fail-closed).
 */
@Component
class Neo4jSystemConfigAdapter implements SystemConfigRepository {

  private static final String KEY = "default_policy";

  private static final String READ = "MATCH (s:SystemConfig {key: $key}) RETURN s.value AS value";

  private static final String WRITE = "MERGE (s:SystemConfig {key: $key}) SET s.value = $value";

  private final Neo4jClient client;

  Neo4jSystemConfigAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public DefaultPolicy defaultPolicy() {
    return client
        .query(READ)
        .bind(KEY)
        .to("key")
        .fetchAs(DefaultPolicy.class)
        .mappedBy((t, row) -> DefaultPolicy.valueOf(row.get("value").asString()))
        .one()
        .orElse(DefaultPolicy.DENY);
  }

  @Override
  public void setDefaultPolicy(DefaultPolicy policy) {
    client.query(WRITE).bindAll(Map.of("key", KEY, "value", policy.name())).run();
  }
}
