package com.knowledgehub.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.eval.EvalHarness.EvalReport;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Asserts the retrieval NFR targets — Recall@10, MRR, and the hybrid-over-semantic margin — against
 * a <em>real</em> embedding provider. Unlike {@link RetrievalEvalTests} it does not stub the
 * embedding model, so it makes live provider calls while indexing the corpus and embedding queries.
 *
 * <p>Indexes a real, small public Git repository (via the same {@code GitConnector} path {@link
 * com.knowledgehub.knowledge.sync.GitSourceLiveIndexingTests} exercises) instead of the curated
 * markdown fixture {@link RetrievalEvalTests} uses — real source code gives BM25 exact-identifier
 * matches a genuine edge over pure semantic similarity, which is what the margin assertion below
 * needs to mean something. The repo ref is pinned to a specific commit so the gold set in {@code
 * eval/gold-set-git.json} (hand-checked against that commit's actual files) stays valid even if the
 * upstream repo changes later.
 *
 * <p>The whole class is gated on {@code EVAL_ASSERT_THRESHOLDS=true} so it never boots in CI: run
 * it with that variable set, a real provider configured ({@code EMBEDDING_API_KEY} and a model
 * whose dimension matches {@code app.embedding.dimension}), and outbound network access to GitHub.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("eval")
@EnabledIfEnvironmentVariable(named = "EVAL_ASSERT_THRESHOLDS", matches = "true")
class RetrievalEvalThresholdTests {

  private static final Logger log = LoggerFactory.getLogger(RetrievalEvalThresholdTests.class);

  private static final String SOURCE = "eval-corpus-git";

  // Same small (~9k LOC) public repo GitSourceLiveIndexingTests clones, pinned to the commit the
  // gold set below was written against.
  private static final String REPO_URL = "https://github.com/sindresorhus/globby.git";
  private static final String REPO_REF = "47e7f658b87c1f48a4e62600a47dc0eca6dae249";

  /**
   * src/test/resources/application.yml is the only {@code classpath:/application.yml} Spring Boot
   * loads for tests (test-classes precedes classes on the test classpath, and Boot resolves that
   * location once, not merged) — so it doesn't just override {@code spring.ai.openai.api-key}, it
   * shadows every other embedding setting from src/main's application.yml too, silently falling
   * back to defaults (dimension 1536, base-url api.openai.com). This class is the one test meant to
   * hit the real provider, so its actual settings are restored here from the environment.
   */
  @DynamicPropertySource
  static void realEmbeddingSettings(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.ai.openai.api-key", () -> System.getenv().getOrDefault("EMBEDDING_API_KEY", ""));
    registry.add(
        "spring.ai.openai.base-url",
        () -> System.getenv().getOrDefault("EMBEDDING_BASE_URL", "https://api.openai.com"));
    registry.add(
        "spring.ai.openai.embedding.options.model",
        () -> System.getenv().getOrDefault("EMBEDDING_MODEL", "text-embedding-3-small"));
    registry.add(
        "app.embedding.dimension",
        () -> System.getenv().getOrDefault("EMBEDDING_DIMENSION", "1536"));
  }

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
    goldSet = EvalHarness.loadGoldSet("eval/gold-set-git.json");
    EvalHarness.indexCorpus(
        indexingService,
        sources,
        new Source(SOURCE, SourceType.GIT, REPO_URL, REPO_REF, List.of(), List.of()));
  }

  @AfterAll
  void cleanUp() {
    EvalHarness.cleanUp(cache, chunks, entities, vectorStore, sources, SOURCE);
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
    // On this corpus, semantic-only already surfaces the right file within top-10 for nearly
    // every query, so Recall@10 saturates for both paths and can't show a hybrid advantage —
    // hybrid's real edge (BM25 nailing exact code identifiers) shows up as a better *rank*, i.e.
    // MRR. Recall must still never regress; MRR margin is the one that has to be positive.
    assertThat(hybrid.recallAt10()).isGreaterThanOrEqualTo(semantic.recallAt10());
    assertThat(hybrid.mrr() - semantic.mrr()).isGreaterThanOrEqualTo(0.03);
  }
}
