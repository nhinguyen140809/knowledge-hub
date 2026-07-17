package com.knowledgehub.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.eval.EvalHarness.EvalReport;
import com.knowledgehub.knowledge.domain.port.EmbeddingPort;
import com.knowledgehub.knowledge.domain.port.VectorStorePort;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.port.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.port.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.port.RetrievalReadPort;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Retrieval quality harness over the labelled gold set: indexes the eval corpus once, runs every
 * query through the hybrid pipeline and the semantic path alone, and reports Recall@10 and MRR so
 * regressions in chunking, fusion or weighting are measured rather than guessed.
 *
 * <p>The embedding provider is stubbed, so semantic ranking is arbitrary and only structural
 * invariants are checked — the harness runs and the lexical-backed hybrid actually retrieves. The
 * target NFR thresholds are asserted against a real provider by {@link
 * RetrievalEvalThresholdTests}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("eval")
class RetrievalEvalTests {

  private static final Logger log = LoggerFactory.getLogger(RetrievalEvalTests.class);

  @MockitoBean private EmbeddingModel embeddingModel;

  @Autowired private IndexingService indexingService;
  @Autowired private RetrievalService retrievalService;
  @Autowired private VectorStorePort vectorStore;
  @Autowired private EmbeddingPort embeddingPort;
  @Autowired private RetrievalReadPort reader;
  @Autowired private RetrievalCache cache;
  @Autowired private ChunkRepository chunks;
  @Autowired private CodeEntityRepository entities;
  @Autowired private SourceRepository sources;

  private List<EvalHarness.EvalCase> goldSet;

  @BeforeAll
  void indexCorpus() {
    // A distinct one-hot vector per text: indexing is deterministic and offline, while semantic
    // ranking stays arbitrary (query and passage never collide) so hybrid rests on the lexical
    // path.
    when(embeddingModel.embed(anyList()))
        .thenAnswer(
            invocation ->
                ((List<String>) invocation.getArgument(0))
                    .stream().map(RetrievalEvalTests::oneHot).toList());
    when(embeddingModel.embed(anyString()))
        .thenAnswer(invocation -> oneHot(invocation.getArgument(0)));

    goldSet = EvalHarness.loadGoldSet();
    EvalHarness.indexCorpus(indexingService, sources);
  }

  @AfterAll
  void cleanUp() {
    EvalHarness.cleanUp(cache, chunks, entities, vectorStore, sources);
  }

  @Test
  void reportsRecallAndMrrForHybridVersusSemantic() {
    EvalReport hybrid =
        EvalHarness.evaluate("hybrid", goldSet, EvalHarness.hybridRanking(retrievalService));
    EvalReport semantic =
        EvalHarness.evaluate(
            "semantic", goldSet, EvalHarness.semanticRanking(vectorStore, embeddingPort, reader));
    EvalHarness.report(log, goldSet.size(), hybrid, semantic);

    assertThat(goldSet).hasSizeGreaterThanOrEqualTo(50);
    for (EvalReport report : List.of(hybrid, semantic)) {
      assertThat(report.recallAt10()).isBetween(0.0, 1.0);
      assertThat(report.mrr()).isBetween(0.0, 1.0);
    }
    // The lexical-backed hybrid must actually retrieve, and never do worse than semantic alone.
    assertThat(hybrid.recallAt10()).isGreaterThan(0.5);
    assertThat(hybrid.recallAt10()).isGreaterThanOrEqualTo(semantic.recallAt10());
  }

  private static float[] oneHot(String text) {
    float[] vector = new float[1536];
    vector[Math.floorMod(text.hashCode(), vector.length)] = 1f;
    return vector;
  }
}
