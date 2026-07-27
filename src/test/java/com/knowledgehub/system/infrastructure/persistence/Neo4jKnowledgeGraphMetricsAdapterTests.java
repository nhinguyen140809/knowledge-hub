package com.knowledgehub.system.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.access.domain.port.PrincipalRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import com.knowledgehub.system.domain.GraphMetrics;
import com.knowledgehub.system.domain.port.KnowledgeGraphMetrics;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Counts by delta rather than absolute value, since the Testcontainers Neo4j instance is shared
 * across the whole run — other tests' fixtures may already be present.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Neo4jKnowledgeGraphMetricsAdapterTests {

  private static final String SRC_ID = "stats-it-source";
  private static final String PRINCIPAL_ID = "stats-it-principal";

  @Autowired private KnowledgeGraphMetrics metrics;
  @Autowired private SourceRepository sources;
  @Autowired private PrincipalRepository principals;

  @AfterEach
  void tearDown() {
    sources.deleteById(SRC_ID);
    principals.deleteById(PRINCIPAL_ID);
  }

  @Test
  void countsKnowledgeGraphNodesButNotAccessControlNodes() {
    GraphMetrics before = metrics.snapshot();

    sources.save(new Source(SRC_ID, SourceType.FS, "/stats", null, List.of(), List.of()));
    principals.save(new Principal(PRINCIPAL_ID, PrincipalType.SUBJECT, Role.MEMBER));

    GraphMetrics after = metrics.snapshot();

    // The new Source is in scope; the new Principal (access control) must not be.
    assertThat(after.graphNodes()).isEqualTo(before.graphNodes() + 1);
  }
}
