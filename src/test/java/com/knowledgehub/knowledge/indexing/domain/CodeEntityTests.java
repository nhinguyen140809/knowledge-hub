package com.knowledgehub.knowledge.indexing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CodeEntityTests {

  @Test
  void buildsATopLevelType() {
    CodeEntity type =
        new CodeEntity(
            "eid", "src", "fid", null, CodeEntityLevel.CLASS, "Foo", "com.x.Foo", "class Foo", 1, 20);
    assertThat(type.parentEntityId()).isNull();
    assertThat(type.level()).isEqualTo(CodeEntityLevel.CLASS);
  }

  @Test
  void rejectsInvalidLineRange() {
    assertThatThrownBy(
            () ->
                new CodeEntity(
                    "eid",
                    "src",
                    "fid",
                    null,
                    CodeEntityLevel.METHOD,
                    "m",
                    "com.x.Foo#m",
                    "void m()",
                    9,
                    3))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
