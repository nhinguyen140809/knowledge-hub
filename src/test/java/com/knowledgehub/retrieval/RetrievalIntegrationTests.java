package com.knowledgehub.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RankedResult;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end retrieval over real Neo4j + Qdrant. Indexes one or two sources, then queries through
 * the full pipeline. Proves a free-text query returns ranked hits with citable metadata, and that
 * the {@code allowedSources} pre-filter keeps a disallowed source out of every path. The embedding
 * provider is mocked deterministically so the test is offline.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RetrievalIntegrationTests {

  private static final String SOURCE_A = "it-query-a";
  private static final String SOURCE_B = "it-query-b";

  @MockitoBean private EmbeddingModel embeddingModel;

  @Autowired private IndexingService indexingService;
  @Autowired private RetrievalService retrievalService;
  @Autowired private RetrievalCache cache;
  @Autowired private ChunkRepository chunks;
  @Autowired private CodeEntityRepository entities;
  @Autowired private VectorStorePort vectorStore;
  @Autowired private SourceRepository sources;

  @TempDir Path root;

  @BeforeEach
  void setUp() throws IOException {
    when(embeddingModel.embed(anyList()))
        .thenAnswer(
            invocation -> {
              List<String> texts = invocation.getArgument(0);
              return texts.stream().map(RetrievalIntegrationTests::deterministicVector).toList();
            });

    Path a = Files.createDirectories(root.resolve("a"));
    Files.writeString(
        a.resolve("Greeter.java"),
        """
        package com.example;

        public class Greeter {
          String greet(String name) {
            return "Hello " + name;
          }
        }
        """);

    Path b = Files.createDirectories(root.resolve("b"));
    Files.writeString(
        b.resolve("Other.java"),
        """
        package com.other;

        public class Other {
          void run() {}
        }
        """);

    sources.save(
        new Source(SOURCE_A, SourceType.FS, a.toString(), null, List.of("**/*.java"), List.of()));
    sources.save(
        new Source(SOURCE_B, SourceType.FS, b.toString(), null, List.of("**/*.java"), List.of()));
  }

  @AfterEach
  void tearDown() {
    cache.invalidateAll();
    for (String source : List.of(SOURCE_A, SOURCE_B)) {
      chunks.deleteBySource(source);
      entities.deleteBySource(source);
      vectorStore.deleteBySource(source);
      sources.deleteById(source);
    }
  }

  @Test
  void returnsRankedHitsWithCitableMetadata() {
    indexingService.index(SOURCE_A);

    RankedResult result = retrievalService.retrieve(Query.of("Greeter greet name"), null);

    assertThat(result.hits()).isNotEmpty();
    assertThat(result.hits())
        .anySatisfy(
            hit -> {
              assertThat(hit.metadata().path()).endsWith("Greeter.java");
              assertThat(hit.metadata().sourceId()).isEqualTo(SOURCE_A);
              assertThat(hit.metadata().lineStart()).isNotNull();
            });
  }

  @Test
  void neverReturnsADisallowedSourceOnAnyPath() {
    indexingService.index(SOURCE_A);
    indexingService.index(SOURCE_B);

    RankedResult onlyB = retrievalService.retrieve(Query.of("Greeter greet"), Set.of(SOURCE_B));
    assertThat(onlyB.hits()).extracting(hit -> hit.metadata().sourceId()).doesNotContain(SOURCE_A);

    RankedResult onlyA = retrievalService.retrieve(Query.of("Greeter greet"), Set.of(SOURCE_A));
    assertThat(onlyA.hits()).isNotEmpty();
    assertThat(onlyA.hits())
        .allSatisfy(hit -> assertThat(hit.metadata().sourceId()).isEqualTo(SOURCE_A));
  }

  private static float[] deterministicVector(String text) {
    float[] vector = new float[1536];
    vector[Math.floorMod(text.hashCode(), vector.length)] = 1f;
    return vector;
  }
}
