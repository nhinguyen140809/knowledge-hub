package com.knowledgehub.shared.id;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashingTests {

  @Test
  void isDeterministicForSameContent() {
    assertThat(Hashing.sha256("hello")).isEqualTo(Hashing.sha256("hello"));
  }

  @Test
  void differsForDifferentContent() {
    assertThat(Hashing.sha256("hello")).isNotEqualTo(Hashing.sha256("world"));
  }

  @Test
  void producesLowercaseHexOf64Chars() {
    assertThat(Hashing.sha256("anything")).hasSize(64).matches("[0-9a-f]{64}");
  }
}
