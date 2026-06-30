package com.knowledgehub.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AssembleResultStageTests {

  private final RetrievalReadPort reader = mock(RetrievalReadPort.class);
  private final AssembleResultStage stage = new AssembleResultStage(reader);

  @Test
  void buildsHitsInFusedOrderAndDropsIdsWithoutMetadata() {
    RetrievalContext context = new RetrievalContext(Query.of("q"), Filter.unrestricted());
    context.setFusedHits(
        List.of(new ScoredId("a", 3), new ScoredId("disallowed", 2), new ScoredId("b", 1)));
    // The metadata load is ACL-scoped, so a disallowed id simply comes back absent and is dropped.
    when(reader.loadMetadata(anyCollection(), any()))
        .thenReturn(Map.of("a", meta("src"), "b", meta("src")));

    stage.apply(context);

    assertThat(context.assembledHits()).extracting(Hit::id).containsExactly("a", "b");
    assertThat(context.assembledHits()).first().extracting(Hit::relevanceScore).isEqualTo(3.0);
  }

  private static HitMetadata meta(String sourceId) {
    return new HitMetadata("chunk", sourceId, "p", 1, 2, "code", "main", null, null, List.of());
  }
}
