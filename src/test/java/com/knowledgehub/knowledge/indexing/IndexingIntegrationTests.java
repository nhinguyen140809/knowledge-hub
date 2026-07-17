package com.knowledgehub.knowledge.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.port.VectorStorePort;
import com.knowledgehub.knowledge.indexing.application.IndexResult;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.port.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.port.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end indexing acceptance: index a filesystem source, then confirm a semantic search returns
 * the right chunk and that re-indexing the same content is idempotent (no duplicate chunks). The
 * embedding provider is mocked with a deterministic text→vector function so the test is offline and
 * stable while still exercising the real chunk → dedup → embed → store path against Neo4j + Qdrant.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IndexingIntegrationTests {

  private static final String SOURCE_ID = "it-index-search";

  @MockitoBean private EmbeddingModel embeddingModel;

  @Autowired private IndexingService indexingService;
  @Autowired private VectorStorePort vectorStore;
  @Autowired private ChunkRepository chunks;
  @Autowired private CodeEntityRepository entities;
  @Autowired private SourceRepository sources;
  @Autowired private Neo4jClient neo4j;

  @TempDir Path repo;

  @BeforeEach
  void setUp() throws IOException {
    when(embeddingModel.embed(anyList()))
        .thenAnswer(
            inv -> {
              List<String> texts = inv.getArgument(0);
              return texts.stream().map(IndexingIntegrationTests::deterministicVector).toList();
            });

    Files.writeString(repo.resolve("alpha.md"), "# Alpha\n\nalpha unique paragraph about cats.\n");
    Files.writeString(repo.resolve("beta.md"), "# Beta\n\nbeta different paragraph about dogs.\n");
    sources.save(
        new Source(SOURCE_ID, SourceType.FS, repo.toString(), null, List.of("**/*.md"), List.of()));
  }

  @AfterEach
  void tearDown() {
    chunks.deleteBySource(SOURCE_ID);
    entities.deleteBySource(SOURCE_ID);
    vectorStore.deleteBySource(SOURCE_ID);
    sources.deleteById(SOURCE_ID);
  }

  @Test
  void indexThenSemanticSearchReturnsTheMatchingChunk() {
    IndexResult result = indexingService.index(SOURCE_ID);
    assertThat(result.chunksIndexed()).isGreaterThanOrEqualTo(2);

    Map<String, Object> stored =
        neo4j
            .query(
                "MATCH (c:Chunk {source_id: $sid, path: 'alpha.md'})"
                    + " RETURN c.chunk_id AS chunkId, c.text AS text LIMIT 1")
            .bind(SOURCE_ID)
            .to("sid")
            .fetch()
            .one()
            .orElseThrow();

    float[] query = deterministicVector((String) stored.get("text"));
    List<ScoredId> hits = vectorStore.search(query, 3, Filter.unrestricted());

    assertThat(hits).isNotEmpty();
    assertThat(hits.get(0).chunkId()).isEqualTo(stored.get("chunkId"));
  }

  @Test
  void reIndexingTheSameContentAddsNoDuplicates() {
    indexingService.index(SOURCE_ID);
    long afterFirst = chunkCount();

    IndexResult second = indexingService.index(SOURCE_ID);

    assertThat(second.chunksIndexed()).isZero();
    assertThat(second.chunksCached()).isGreaterThanOrEqualTo(2);
    assertThat(chunkCount()).isEqualTo(afterFirst);
  }

  private long chunkCount() {
    return neo4j
        .query("MATCH (c:Chunk {source_id: $sid}) RETURN count(c) AS n")
        .bind(SOURCE_ID)
        .to("sid")
        .fetchAs(Long.class)
        .one()
        .orElse(0L);
  }

  /**
   * A stable 1536-dim unit vector keyed off the text, so identical text yields identical vectors.
   */
  private static float[] deterministicVector(String text) {
    float[] vector = new float[1536];
    vector[Math.floorMod(text.hashCode(), vector.length)] = 1f;
    return vector;
  }
}
