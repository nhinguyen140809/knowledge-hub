package com.knowledgehub.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.eval.EvalHarness.EvalReport;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Asserts the retrieval NFR targets — Recall@10, MRR, and the hybrid-over-semantic margin — against
 * a <em>real</em> embedding provider. Unlike {@link RetrievalEvalTests} it does not stub the
 * embedding model, so it makes live provider calls while indexing the corpus and embedding queries.
 *
 * <p>The whole class is gated on {@code EVAL_ASSERT_THRESHOLDS=true} so it never boots in CI: run
 * it with that variable set and a real provider configured ({@code OPENAI_API_KEY} and a model
 * whose dimension matches {@code app.embedding.dimension}).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("eval")
@EnabledIfEnvironmentVariable(named = "EVAL_ASSERT_THRESHOLDS", matches = "true")
class RetrievalEvalThresholdTests {

  private static final Logger log = LoggerFactory.getLogger(RetrievalEvalThresholdTests.class);

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
    goldSet = EvalHarness.loadGoldSet();
    EvalHarness.indexCorpus(indexingService, sources);
  }

  @AfterAll
  void cleanUp() {
    EvalHarness.cleanUp(cache, chunks, entities, vectorStore, sources);
  }

  @Test
  void meetsRetrievalTargets() {
    EvalReport hybrid =
        EvalHarness.evaluate("hybrid", goldSet, EvalHarness.hybridRanking(retrievalService));
    EvalReport semantic =
        EvalHarness.evaluate(
            "semantic", goldSet, EvalHarness.semanticRanking(vectorStore, embeddingPort, reader));
    EvalHarness.report(log, goldSet.size(), hybrid, semantic);

    assertThat(hybrid.recallAt10()).isGreaterThanOrEqualTo(0.85);
    assertThat(hybrid.mrr()).isGreaterThanOrEqualTo(0.70);
    assertThat(hybrid.recallAt10() - semantic.recallAt10()).isGreaterThanOrEqualTo(0.05);
  }
}
