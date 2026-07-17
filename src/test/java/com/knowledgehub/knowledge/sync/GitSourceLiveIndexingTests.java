package com.knowledgehub.knowledge.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import com.knowledgehub.knowledge.sync.application.SyncService;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import com.knowledgehub.knowledge.sync.domain.port.Evictor;
import com.knowledgehub.knowledge.sync.domain.port.FreshnessRepository;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end Git indexing against a real, unmodified public repository — proves the {@code
 * GitConnector} clone path and {@code CommitIndexingService} work against real history, not just
 * the small fixtures {@code GitConnectorTests} builds in a temp dir. The embedding provider stays
 * mocked (deterministic, offline); the only external dependency is the clone itself, which is why
 * this is opt-in rather than part of the default suite: run it with {@code GIT_LIVE_TEST=true}, and
 * only where the sandbox allows outbound network access to GitHub.
 *
 * <p>Cleans up after itself in {@link #tearDown()} regardless of test outcome, the same as {@link
 * SyncIntegrationTests} — nothing about the target repo is touched (Git only clones it into a temp
 * dir), and nothing survives in this app's Neo4j/Qdrant once the test finishes.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("network")
@EnabledIfEnvironmentVariable(named = "GIT_LIVE_TEST", matches = "true")
class GitSourceLiveIndexingTests {

  private static final String SOURCE = "it-git-live";

  // Real, small (~9k LOC) public repo picked for this test: no build step, no submodules, fast to
  // clone, unlikely to disappear. Any similarly small public repo would do just as well.
  private static final String REPO_URL = "https://github.com/sindresorhus/globby.git";

  @MockitoBean private EmbeddingModel embeddingModel;

  @Autowired private SyncService syncService;
  @Autowired private SourceRepository sources;
  @Autowired private Evictor evictor;
  @Autowired private FreshnessRepository freshness;
  @Autowired private RetrievalCache cache;
  @Autowired private Neo4jClient neo4jClient;

  @BeforeEach
  void setUp() {
    when(embeddingModel.embed(anyList()))
        .thenAnswer(
            invocation -> {
              List<String> texts = invocation.getArgument(0);
              return texts.stream().map(GitSourceLiveIndexingTests::deterministicVector).toList();
            });
    sources.save(new Source(SOURCE, SourceType.GIT, REPO_URL, null, List.of(), List.of()));
  }

  @AfterEach
  void tearDown() {
    evictor.evictSource(SOURCE);
    freshness.deleteBySource(SOURCE);
    sources.deleteById(SOURCE);
    cache.invalidateAll();
  }

  @Test
  void indexesARealRepositoryWithRealCommitHashes() {
    SyncResult result = syncService.sync(SOURCE);

    assertThat(result.indexed()).isGreaterThan(0);
    assertThat(result.commitsIndexed()).isGreaterThan(0);
    assertThat(result.toCommit()).matches("[0-9a-f]{40}");

    List<Map<String, Object>> commitShas =
        neoRows("MATCH (c:Commit {source_id: $sourceId}) RETURN c.sha AS sha LIMIT 5");
    assertThat(commitShas).isNotEmpty();
    assertThat(commitShas)
        .allSatisfy(row -> assertThat((String) row.get("sha")).matches("[0-9a-f]{40}"));

    long fileCount =
        (Long)
            neoRows("MATCH (f:File {source_id: $sourceId}) RETURN count(f) AS n").get(0).get("n");
    assertThat(fileCount).isGreaterThan(0);
  }

  private List<Map<String, Object>> neoRows(String cypher) {
    return neo4jClient.query(cypher).bind(SOURCE).to("sourceId").fetch().all().stream().toList();
  }

  private static float[] deterministicVector(String text) {
    float[] vector = new float[1536];
    vector[Math.floorMod(text.hashCode(), vector.length)] = 1f;
    return vector;
  }
}
