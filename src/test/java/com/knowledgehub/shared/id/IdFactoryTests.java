package com.knowledgehub.shared.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdFactoryTests {

  @Test
  void sameInputsYieldSameId() {
    assertThat(IdFactory.chunkId("src-1", "a/b.java", "hash1"))
        .isEqualTo(IdFactory.chunkId("src-1", "a/b.java", "hash1"));
  }

  @Test
  void differentInputsYieldDifferentId() {
    assertThat(IdFactory.chunkId("src-1", "a/b.java", "hash1"))
        .isNotEqualTo(IdFactory.chunkId("src-1", "a/b.java", "hash2"));
  }

  @Test
  void partBoundariesDoNotCollide() {
    // With a naive space separator these would both join to "a b c" and collide.
    assertThat(IdFactory.stableId("a b", "c")).isNotEqualTo(IdFactory.stableId("a", "b c"));
  }

  @Test
  void rejectsEmptyParts() {
    assertThatThrownBy(IdFactory::stableId).isInstanceOf(IllegalArgumentException.class);
  }
}
