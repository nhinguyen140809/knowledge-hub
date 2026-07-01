package com.knowledgehub.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Retrieval quality harness: indexes a labelled corpus once, runs every gold query through the
 * hybrid pipeline and through the semantic path alone, and reports Recall@10 and MRR for each so
 * regressions in chunking, fusion or weighting are measured rather than guessed. Prints a metrics
 * table an operator can read at a glance.
 *
 * <p>In the normal build the embedding provider is stubbed, so semantic ranking is arbitrary and
 * only structural invariants are checked (the harness runs, the lexical-backed hybrid retrieves).
 * To assert the target thresholds, point the app at a real embedding provider and run with {@code
 * EVAL_ASSERT_THRESHOLDS=true}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("eval")
class RetrievalEvalTests {

  private static final Logger log = LoggerFactory.getLogger(RetrievalEvalTests.class);

  private static final String SOURCE = "eval-corpus";
  private static final Path CORPUS = Path.of("src/test/resources/eval/corpus").toAbsolutePath();

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

  private List<EvalCase> goldSet;

  @BeforeAll
  void indexCorpus() throws Exception {
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

    goldSet = loadGoldSet();
    sources.save(new Source(SOURCE, SourceType.FS, CORPUS.toString(), null, List.of(), List.of()));
    indexingService.index(SOURCE);
  }

  @AfterAll
  void cleanUp() {
    cache.invalidateAll();
    chunks.deleteBySource(SOURCE);
    entities.deleteBySource(SOURCE);
    vectorStore.deleteBySource(SOURCE);
    sources.deleteById(SOURCE);
  }

  @Test
  void reportsRecallAndMrrForHybridVersusSemantic() {
    EvalReport hybrid = evaluate("hybrid", this::hybridRanking);
    EvalReport semantic = evaluate("semantic", this::semanticRanking);
    print(hybrid, semantic);

    assertThat(goldSet).hasSizeGreaterThanOrEqualTo(50);
    for (EvalReport report : List.of(hybrid, semantic)) {
      assertThat(report.recallAt10()).isBetween(0.0, 1.0);
      assertThat(report.mrr()).isBetween(0.0, 1.0);
    }
    // The lexical-backed hybrid must actually retrieve, and never do worse than semantic alone.
    assertThat(hybrid.recallAt10()).isGreaterThan(0.5);
    assertThat(hybrid.recallAt10()).isGreaterThanOrEqualTo(semantic.recallAt10());
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "EVAL_ASSERT_THRESHOLDS", matches = "true")
  void meetsRetrievalTargetsWithARealEmbeddingProvider() {
    EvalReport hybrid = evaluate("hybrid", this::hybridRanking);
    EvalReport semantic = evaluate("semantic", this::semanticRanking);
    print(hybrid, semantic);

    assertThat(hybrid.recallAt10()).isGreaterThanOrEqualTo(0.85);
    assertThat(hybrid.mrr()).isGreaterThanOrEqualTo(0.70);
    assertThat(hybrid.recallAt10() - semantic.recallAt10()).isGreaterThanOrEqualTo(0.05);
  }

  private EvalReport evaluate(String name, Function<String, List<String>> ranking) {
    double recallSum = 0;
    double mrrSum = 0;
    for (EvalCase item : goldSet) {
      List<String> paths = ranking.apply(item.query());
      int rank = 0;
      for (int i = 0; i < Math.min(10, paths.size()); i++) {
        if (paths.get(i).endsWith(item.relevantPath())) {
          rank = i + 1;
          break;
        }
      }
      if (rank > 0) {
        recallSum += 1;
        mrrSum += 1.0 / rank;
      }
    }
    int n = goldSet.size();
    return new EvalReport(name, recallSum / n, mrrSum / n);
  }

  private List<String> hybridRanking(String query) {
    return retrievalService.retrieve(Query.of(query), null).hits().stream()
        .map(hit -> hit.metadata().path())
        .toList();
  }

  private List<String> semanticRanking(String query) {
    List<ScoredId> scored =
        vectorStore.search(embeddingPort.embed(query), 10, Filter.unrestricted());
    Map<String, HitMetadata> metadata =
        reader.loadMetadata(scored.stream().map(ScoredId::chunkId).toList(), Filter.unrestricted());
    return scored.stream()
        .map(hit -> metadata.get(hit.chunkId()))
        .filter(Objects::nonNull)
        .map(HitMetadata::path)
        .toList();
  }

  private void print(EvalReport hybrid, EvalReport semantic) {
    log.info("Retrieval eval over {} gold queries:", goldSet.size());
    log.info(String.format("  %-9s | Recall@10 | MRR", "strategy"));
    for (EvalReport report : List.of(hybrid, semantic)) {
      log.info(
          String.format(
              "  %-9s |    %.3f  | %.3f", report.name(), report.recallAt10(), report.mrr()));
    }
  }

  private List<EvalCase> loadGoldSet() throws Exception {
    try (InputStream in = new ClassPathResource("eval/gold-set.json").getInputStream()) {
      return new ObjectMapper().readValue(in, new TypeReference<List<EvalCase>>() {});
    }
  }

  private static float[] oneHot(String text) {
    float[] vector = new float[1536];
    vector[Math.floorMod(text.hashCode(), vector.length)] = 1f;
    return vector;
  }

  private record EvalCase(String id, String query, String relevantPath) {}

  private record EvalReport(String name, double recallAt10, double mrr) {}
}
