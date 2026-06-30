package com.knowledgehub.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.retrieval.domain.GraphTraversalPort;
import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.KeywordSearchPort;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.QueryParams;
import com.knowledgehub.retrieval.domain.RankedResult;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import com.knowledgehub.retrieval.infrastructure.fusion.RrfFusion;
import com.knowledgehub.shared.config.AppProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Wires the real stages with mocked ports and an inline executor, so the orchestration - parallel
 * fan-out, fusion, assembly, filtering, canonical-ref fallback, degradation and caching - is
 * exercised end to end without a database.
 */
class RetrievalServiceTests {

  private final EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
  private final VectorStorePort vectorStore = mock(VectorStorePort.class);
  private final KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
  private final GraphTraversalPort graphTraversal = mock(GraphTraversalPort.class);
  private final RetrievalReadPort reader = mock(RetrievalReadPort.class);

  private RetrievalService service;

  @BeforeEach
  void setUp() {
    AppProperties properties = new AppProperties(null, null, null, null);
    service =
        new RetrievalService(
            new PrepareQueryStage(),
            new SemanticSearchStage(vectorStore, embeddingPort, properties),
            new KeywordSearchStage(keywordSearch, properties),
            new GraphTraversalStage(graphTraversal, properties),
            new RrfFusionStage(new RrfFusion(), properties),
            new AssembleResultStage(reader),
            new AclFilterStage(),
            reader,
            new RetrievalCache(properties),
            properties,
            Runnable::run);

    when(embeddingPort.embed(any())).thenReturn(new float[] {1f});
    when(graphTraversal.expand(anyCollection(), anyInt(), any())).thenReturn(List.of());
  }

  @Test
  void fusesTheTwoPathsAndAssemblesRankedHits() {
    when(vectorStore.search(any(), anyInt(), any()))
        .thenReturn(List.of(new ScoredId("a", 0.9), new ScoredId("b", 0.8)));
    when(keywordSearch.search(anyList(), anyInt(), any()))
        .thenReturn(List.of(new ScoredId("b", 5), new ScoredId("c", 4)));
    when(reader.loadMetadata(anyCollection(), any()))
        .thenReturn(Map.of("a", meta(), "b", meta(), "c", meta()));

    RankedResult result = service.retrieve(Query.of("find the greeter"), null);

    // b is the only id both paths rank, so fusion lifts it to the top.
    assertThat(result.hits()).extracting(Hit::id).first().isEqualTo("b");
    assertThat(result.hits()).extracting(Hit::id).containsExactlyInAnyOrder("a", "b", "c");
    assertThat(result.servedFromCanonicalRef()).isFalse();
  }

  @Test
  void degradesWhenOnePathFailsInsteadOfFailingTheQuery() {
    when(vectorStore.search(any(), anyInt(), any()))
        .thenThrow(new IllegalStateException("vector store down"));
    when(keywordSearch.search(anyList(), anyInt(), any()))
        .thenReturn(List.of(new ScoredId("c", 4)));
    when(reader.loadMetadata(anyCollection(), any())).thenReturn(Map.of("c", meta()));

    RankedResult result = service.retrieve(Query.of("find the greeter"), null);

    assertThat(result.hits()).extracting(Hit::id).containsExactly("c");
  }

  @Test
  void degradesSemanticWhenEmbeddingFailsButStillServesKeyword() {
    when(embeddingPort.embed(any()))
        .thenThrow(new IllegalStateException("embedding provider down"));
    when(keywordSearch.search(anyList(), anyInt(), any()))
        .thenReturn(List.of(new ScoredId("c", 4)));
    when(reader.loadMetadata(anyCollection(), any())).thenReturn(Map.of("c", meta()));

    RankedResult result = service.retrieve(Query.of("find the greeter"), null);

    // Embedding lives in the semantic path now, so its outage drops only that path.
    assertThat(result.hits()).extracting(Hit::id).containsExactly("c");
  }

  @Test
  void fallsBackToCanonicalRefWhenTheRequestedRefIsNotIndexed() {
    when(vectorStore.search(any(), anyInt(), any())).thenReturn(List.of(new ScoredId("a", 0.9)));
    when(keywordSearch.search(anyList(), anyInt(), any())).thenReturn(List.of());
    when(reader.loadMetadata(anyCollection(), any())).thenReturn(Map.of("a", meta()));
    when(reader.refIndexed(any(), any())).thenReturn(false);

    RankedResult result =
        service.retrieve(new Query("find it", new QueryParams(null, "feature-x", null)), null);

    assertThat(result.servedFromCanonicalRef()).isTrue();
    assertThat(result.hits()).extracting(Hit::id).containsExactly("a");
  }

  @Test
  void cachesResultsForARepeatedQuery() {
    when(vectorStore.search(any(), anyInt(), any())).thenReturn(List.of(new ScoredId("a", 0.9)));
    when(keywordSearch.search(anyList(), anyInt(), any())).thenReturn(List.of());
    when(reader.loadMetadata(anyCollection(), any())).thenReturn(Map.of("a", meta()));

    service.retrieve(Query.of("same query"), null);
    service.retrieve(Query.of("same query"), null);

    // The second call is a cache hit, so the pipeline (and its embedding) runs only once.
    verify(embeddingPort, times(1)).embed(any());
  }

  private static HitMetadata meta() {
    return new HitMetadata("chunk", "src", "Foo.java", 1, 5, "code", "main", null, null, List.of());
  }
}
