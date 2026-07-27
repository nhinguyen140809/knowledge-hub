package com.knowledgehub.system.application;

import com.knowledgehub.system.domain.DependencyState;
import com.knowledgehub.system.domain.DependencyStatus;
import java.util.List;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

/**
 * Normalizes the actuator health contributors this service tracks into a flat per-dependency
 * status: Neo4j's is auto-registered by Spring Boot; Qdrant's and the embedding provider's are this
 * feature's own {@link HealthIndicator} beans.
 */
@Service
public class DependencyHealthService {

  private static final List<String> TRACKED = List.of("neo4j", "qdrant", "embeddings");

  private final HealthContributorRegistry registry;

  public DependencyHealthService(HealthContributorRegistry registry) {
    this.registry = registry;
  }

  public List<DependencyStatus> currentStatus() {
    return TRACKED.stream().map(this::statusOf).toList();
  }

  private DependencyStatus statusOf(String name) {
    HealthContributor contributor = registry.getContributor(name);
    if (!(contributor instanceof HealthIndicator indicator)) {
      return new DependencyStatus(name, DependencyState.UNKNOWN);
    }
    return new DependencyStatus(name, mapStatus(indicator.health().getStatus()));
  }

  private static DependencyState mapStatus(Status status) {
    if (Status.UP.equals(status)) {
      return DependencyState.UP;
    }
    if (Status.DOWN.equals(status) || Status.OUT_OF_SERVICE.equals(status)) {
      return DependencyState.DOWN;
    }
    return DependencyState.UNKNOWN;
  }
}
