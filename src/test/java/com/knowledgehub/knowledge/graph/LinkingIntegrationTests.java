package com.knowledgehub.knowledge.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.graph.domain.RelationshipRepository;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
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
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end knowledge-linking acceptance across two sources of the same product. Source A holds a
 * base class; source B holds a subclass and a document that both point at A's code. After indexing
 * A then B, the graph must carry the structural relations within A, a cross-source EXTENDS and a
 * confident cross-artifact DESCRIBES from B into A, and re-linking must not duplicate edges. The
 * embedding provider is mocked deterministically so the test is offline.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LinkingIntegrationTests {

  private static final String SOURCE_A = "it-link-a";
  private static final String SOURCE_B = "it-link-b";

  @MockitoBean private EmbeddingModel embeddingModel;

  @Autowired private IndexingService indexingService;
  @Autowired private RelationshipRepository relationships;
  @Autowired private ChunkRepository chunks;
  @Autowired private CodeEntityRepository entities;
  @Autowired private VectorStorePort vectorStore;
  @Autowired private SourceRepository sources;
  @Autowired private Neo4jClient neo4j;

  @TempDir Path root;

  @BeforeEach
  void setUp() throws IOException {
    when(embeddingModel.embed(anyList()))
        .thenAnswer(
            inv -> {
              List<String> texts = inv.getArgument(0);
              return texts.stream().map(LinkingIntegrationTests::deterministicVector).toList();
            });

    Path a = Files.createDirectories(root.resolve("a"));
    Files.writeString(
        a.resolve("Base.java"),
        """
        package com.example;

        public class Base {
          void seed() {}

          void run() {
            seed();
          }
        }
        """);

    Path b = Files.createDirectories(root.resolve("b"));
    Files.writeString(
        b.resolve("Child.java"),
        """
        package com.example;

        public class Child extends Base {
        }
        """);
    Files.writeString(
        b.resolve("README.md"), "# Guide\n\nThe com.example.Base class is the core entry point.\n");

    sources.save(
        new Source(SOURCE_A, SourceType.FS, a.toString(), null, List.of("**/*.java"), List.of()));
    sources.save(
        new Source(
            SOURCE_B,
            SourceType.FS,
            b.toString(),
            null,
            List.of("**/*.java", "**/*.md"),
            List.of()));
  }

  @AfterEach
  void tearDown() {
    for (String source : List.of(SOURCE_A, SOURCE_B)) {
      relationships.deleteBySource(source);
      chunks.deleteBySource(source);
      entities.deleteBySource(source);
      vectorStore.deleteBySource(source);
      sources.deleteById(source);
    }
  }

  private long count(String cypher) {
    return neo4j.query(cypher).fetchAs(Long.class).one().orElse(0L);
  }

  @Test
  void buildsStructuralAndCrossSourceRelations() {
    indexingService.index(SOURCE_A);
    indexingService.index(SOURCE_B);

    // Same-type call inside source A: run() -> seed().
    assertThat(
            count(
                "MATCH (:CodeEntity {source_id: '"
                    + SOURCE_A
                    + "'})-[r:CALLS]->(:CodeEntity {source_id: '"
                    + SOURCE_A
                    + "'}) RETURN count(r)"))
        .isGreaterThanOrEqualTo(1);

    // Cross-source inheritance: Child in B extends Base in A.
    assertThat(
            count(
                "MATCH (:CodeEntity {source_id: '"
                    + SOURCE_B
                    + "'})-[r:EXTENDS]->(:CodeEntity {source_id: '"
                    + SOURCE_A
                    + "'}) RETURN count(r)"))
        .isEqualTo(1);

    // Cross-artifact, cross-source: README in B describes Base in A, with a confidence.
    Double confidence =
        neo4j
            .query(
                "MATCH (:Chunk {source_id: '"
                    + SOURCE_B
                    + "'})-[r:DESCRIBES]->(:CodeEntity {source_id: '"
                    + SOURCE_A
                    + "'}) RETURN r.confidence AS c LIMIT 1")
            .fetchAs(Double.class)
            .mappedBy((t, rec) -> rec.get("c").asDouble())
            .one()
            .orElseThrow();
    assertThat(confidence).isGreaterThanOrEqualTo(0.5);
  }

  @Test
  void reLinkingDoesNotDuplicateEdges() {
    indexingService.index(SOURCE_A);
    indexingService.index(SOURCE_B);
    indexingService.index(SOURCE_B);

    assertThat(
            count(
                "MATCH (:CodeEntity {source_id: '"
                    + SOURCE_B
                    + "'})-[r:EXTENDS]->() RETURN count(r)"))
        .isEqualTo(1);
  }

  private static float[] deterministicVector(String text) {
    float[] vector = new float[1536];
    vector[Math.floorMod(text.hashCode(), vector.length)] = 1f;
    return vector;
  }
}
