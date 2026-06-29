package com.knowledgehub.shared.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineTests {

  @Test
  void runsStagesInOrderThreadingTheContext() {
    Stage<String> append1 = ctx -> ctx + "1";
    Stage<String> append2 = ctx -> ctx + "2";
    Stage<String> append3 = ctx -> ctx + "3";

    Pipeline<String> pipeline = new Pipeline<>(List.of(append1, append2, append3));

    assertThat(pipeline.run("start:")).isEqualTo("start:123");
  }

  @Test
  void emptyPipelineReturnsContextUnchanged() {
    Pipeline<String> pipeline = new Pipeline<>(List.of());

    assertThat(pipeline.run("unchanged")).isEqualTo("unchanged");
  }
}
