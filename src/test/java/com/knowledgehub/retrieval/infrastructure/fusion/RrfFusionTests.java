package com.knowledgehub.retrieval.infrastructure.fusion;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.RankedList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RrfFusionTests {

  private final RrfFusion fusion = new RrfFusion();

  @Test
  void rewardsItemsRankedInMoreThanOneList() {
    RankedList semantic =
        new RankedList("semantic", 1.0, List.of(new ScoredId("a", 9), new ScoredId("b", 8)));
    RankedList keyword =
        new RankedList("keyword", 1.0, List.of(new ScoredId("b", 5), new ScoredId("c", 4)));

    List<ScoredId> fused = fusion.fuse(List.of(semantic, keyword), 60);

    // b is the only id both paths rank, so it wins despite not topping either list alone.
    assertThat(fused).first().extracting(ScoredId::chunkId).isEqualTo("b");
    assertThat(fused).extracting(ScoredId::chunkId).containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  void isReproducibleAndBreaksTiesById() {
    RankedList first = new RankedList("x", 1.0, List.of(new ScoredId("z", 1)));
    RankedList second = new RankedList("y", 1.0, List.of(new ScoredId("a", 1)));

    // z and a each rank first in one list, so their fused scores tie and the id breaks it.
    assertThat(fusion.fuse(List.of(first, second), 60))
        .extracting(ScoredId::chunkId)
        .containsExactly("a", "z");
  }

  @Test
  void weightLiftsAPathAboveAnEqualRankedOne() {
    RankedList strong = new RankedList("keyword", 5.0, List.of(new ScoredId("k", 1)));
    RankedList weak = new RankedList("semantic", 1.0, List.of(new ScoredId("s", 1)));

    assertThat(fusion.fuse(List.of(strong, weak), 60))
        .first()
        .extracting(ScoredId::chunkId)
        .isEqualTo("k");
  }
}
