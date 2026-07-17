package com.knowledgehub.knowledge.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChunkConfigTests {

  @Test
  void acceptsOverlapSmallerThanMax() {
    ChunkConfig config = new ChunkConfig(512, 64);
    assertThat(config.maxTokens()).isEqualTo(512);
    assertThat(config.overlap()).isEqualTo(64);
  }

  @Test
  void rejectsOverlapNotSmallerThanMax() {
    assertThatThrownBy(() -> new ChunkConfig(100, 100))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositiveMax() {
    assertThatThrownBy(() -> new ChunkConfig(0, 0)).isInstanceOf(IllegalArgumentException.class);
  }
}
