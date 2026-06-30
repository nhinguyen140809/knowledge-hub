package com.knowledgehub.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.Query;
import java.util.List;
import org.junit.jupiter.api.Test;

class AclFilterStageTests {

  private final AclFilterStage stage = new AclFilterStage();

  @Test
  void filtersByRefAndTypeThenTruncatesToTopK() {
    RetrievalContext context = context(2, "main", "code");
    context.setAssembledHits(
        List.of(
            hit("a", "main", "code"),
            hit("b", "dev", "code"), // wrong ref -> dropped
            hit("c", "main", "doc"), // wrong type -> dropped
            hit("d", "main", "code"),
            hit("e", "main", "code"))); // beyond top-k

    stage.apply(context);

    assertThat(context.result().hits()).extracting(Hit::id).containsExactly("a", "d");
  }

  @Test
  void keepsEverythingUpToTopKWhenNoFilters() {
    RetrievalContext context = context(10, null, null);
    context.setAssembledHits(List.of(hit("a", "main", "code"), hit("b", "dev", "doc")));

    stage.apply(context);

    assertThat(context.result().hits()).extracting(Hit::id).containsExactly("a", "b");
  }

  @Test
  void carriesTheCanonicalRefFlagIntoTheResult() {
    RetrievalContext context = context(10, null, null);
    context.setServedFromCanonicalRef(true);
    context.setAssembledHits(List.of());

    stage.apply(context);

    assertThat(context.result().servedFromCanonicalRef()).isTrue();
  }

  private static RetrievalContext context(int topK, String ref, String type) {
    RetrievalContext context = new RetrievalContext(Query.of("q"), Filter.unrestricted());
    context.setTopK(topK);
    context.setEffectiveRef(ref);
    context.setTypeFilter(type);
    return context;
  }

  private static Hit hit(String id, String ref, String type) {
    return new Hit(
        id, 1.0, new HitMetadata("chunk", "src", "p", 1, 2, type, ref, null, null, List.of()));
  }
}
