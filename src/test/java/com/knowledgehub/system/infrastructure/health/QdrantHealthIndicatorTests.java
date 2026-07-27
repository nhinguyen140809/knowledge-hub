package com.knowledgehub.system.infrastructure.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QdrantHealthIndicatorTests {

  @Autowired private QdrantHealthIndicator indicator;

  @Test
  void reportsUpWhenQdrantIsReachable() {
    assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
  }
}
