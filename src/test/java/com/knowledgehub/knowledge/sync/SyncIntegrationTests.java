package com.knowledgehub.knowledge.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.sync.application.SyncService;
import com.knowledgehub.knowledge.sync.domain.Evictor;
import com.knowledgehub.knowledge.sync.domain.FreshnessRepository;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
 * End-to-end sync over real Neo4j + Qdrant on a filesystem source. Proves the acceptance criteria:
 * a first sync indexes everything and a re-sync with no change is a no-op; deleting a file evicts
 * it from queries; modifying a file re-indexes the change; and freshness is recorded. The embedding
 * provider is mocked deterministically so the test is offline.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SyncIntegrationTests {

  private static final String SOURCE = "it-sync";

  @MockitoBean private EmbeddingModel embeddingModel;

  @Autowired private SyncService syncService;
  @Autowired private RetrievalService retrievalService;
  @Autowired private SourceRepository sources;
  @Autowired private Evictor evictor;
  @Autowired private FreshnessRepository freshness;
  @Autowired private RetrievalCache cache;

  @TempDir Path root;
  private Path repo;

  @BeforeEach
  void setUp() throws IOException {
    when(embeddingModel.embed(anyList()))
        .thenAnswer(
            invocation -> {
              List<String> texts = invocation.getArgument(0);
              return texts.stream().map(SyncIntegrationTests::deterministicVector).toList();
            });

    repo = Files.createDirectories(root.resolve("repo"));
    Files.writeString(
        repo.resolve("Greeter.java"),
        """
        package com.example;

        public class Greeter {
          String greet(String name) {
            return "Hello " + name;
          }
        }
        """);
    Files.writeString(
        repo.resolve("Helper.java"),
        """
        package com.example;

        public class Helper {
          void help() {}
        }
        """);
    sources.save(
        new Source(SOURCE, SourceType.FS, repo.toString(), null, List.of("**/*.java"), List.of()));
  }

  @AfterEach
  void tearDown() {
    evictor.evictSource(SOURCE);
    freshness.deleteBySource(SOURCE);
    sources.deleteById(SOURCE);
    cache.invalidateAll();
  }

  @Test
  void firstSyncIndexesEverythingAndAReSyncIsANoOp() {
    SyncResult first = syncService.sync(SOURCE);
    assertThat(first.idempotent()).isFalse();
    assertThat(first.indexed()).isEqualTo(2);

    SyncResult second = syncService.sync(SOURCE);
    assertThat(second.idempotent()).isTrue();
    assertThat(second.indexed()).isZero();
    assertThat(second.reindexed()).isZero();
    assertThat(second.evicted()).isZero();
  }

  @Test
  void deletingAFileEvictsItFromQueries() throws IOException {
    syncService.sync(SOURCE);
    assertThat(retrievalService.retrieve(Query.of("Greeter greet"), null).hits())
        .anyMatch(hit -> hit.metadata().path().endsWith("Greeter.java"));

    Files.delete(repo.resolve("Greeter.java"));
    SyncResult result = syncService.sync(SOURCE);

    assertThat(result.evicted()).isGreaterThanOrEqualTo(1);
    assertThat(retrievalService.retrieve(Query.of("Greeter greet"), null).hits())
        .noneMatch(hit -> hit.metadata().path().endsWith("Greeter.java"));
  }

  @Test
  void modifyingAFileReindexesTheChange() throws IOException {
    syncService.sync(SOURCE);

    Files.writeString(
        repo.resolve("Helper.java"),
        """
        package com.example;

        public class Helper {
          void help() {}

          void sprocket() {}
        }
        """);
    SyncResult result = syncService.sync(SOURCE);

    assertThat(result.reindexed()).isEqualTo(1);
    assertThat(result.indexed()).isZero();
    assertThat(retrievalService.retrieve(Query.of("sprocket"), null).hits()).isNotEmpty();
  }

  @Test
  void recordsFreshnessAfterSync() {
    syncService.sync(SOURCE);

    assertThat(syncService.freshnessOf(SOURCE))
        .hasValueSatisfying(info -> assertThat(info.indexedAt()).isNotNull());
  }

  private static float[] deterministicVector(String text) {
    float[] vector = new float[1536];
    vector[Math.floorMod(text.hashCode(), vector.length)] = 1f;
    return vector;
  }
}
