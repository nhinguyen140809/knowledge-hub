package com.knowledgehub.knowledge.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FilterTests {

  @Test
  void unrestrictedMeansEverySourceReadable() {
    Filter filter = Filter.unrestricted();

    assertThat(filter.isUnrestricted()).isTrue();
    assertThat(filter.allowedSources()).isNull();
  }

  @Test
  void ofSourcesRestrictsToTheGivenSet() {
    Filter filter = Filter.ofSources(Set.of("src-1", "src-2"));

    assertThat(filter.isUnrestricted()).isFalse();
    assertThat(filter.allowedSources()).containsExactlyInAnyOrder("src-1", "src-2");
  }

  @Test
  void emptyAllowedSetMeansNothingReadable() {
    Filter filter = Filter.ofSources(Set.of());

    assertThat(filter.isUnrestricted()).isFalse();
    assertThat(filter.allowedSources()).isEmpty();
  }
}
