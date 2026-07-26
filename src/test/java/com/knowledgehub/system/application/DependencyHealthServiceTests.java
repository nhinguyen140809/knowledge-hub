package com.knowledgehub.system.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.knowledgehub.system.domain.DependencyState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;

class DependencyHealthServiceTests {

  @Test
  void mapsUpDownAndAMissingContributorToTheirStates() {
    Map<String, HealthContributor> contributors =
        Map.of(
            "neo4j", (HealthIndicator) () -> Health.up().build(),
            "qdrant", (HealthIndicator) () -> Health.down().build());
    // "embeddings" is deliberately absent — an unregistered name must read as UNKNOWN, not error.
    DependencyHealthService service =
        new DependencyHealthService(new DefaultHealthContributorRegistry(contributors));

    List<?> statuses = service.currentStatus();

    assertThat(statuses)
        .extracting("name", "status")
        .containsExactly(
            tuple("neo4j", DependencyState.UP),
            tuple("qdrant", DependencyState.DOWN),
            tuple("embeddings", DependencyState.UNKNOWN));
  }
}
