package com.knowledgehub.knowledge.graph.infrastructure.link;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CrossArtifactLinkerTests {

  private static final String SOURCE = "src";
  private static final String DOC_PATH = "README.md";

  private final EntityResolver resolver =
      new EntityResolver() {
        @Override
        public Optional<String> resolve(String qualifiedName, ResolutionScope scope) {
          return "com.example.Greeter".equals(qualifiedName)
              ? Optional.of("E:greeter")
              : Optional.empty();
        }

        @Override
        public List<String> findByName(String simpleName, ResolutionScope scope) {
          return Map.of("Greeter", List.of("E:greeter"), "Foo", List.of("F1", "F2"))
              .getOrDefault(simpleName, List.of());
        }

        @Override
        public List<String> findByPath(String path, ResolutionScope scope) {
          return "src/Greeter.java".equals(path) ? List.of("E:greeter") : List.of();
        }
      };

  private static RawArtifact doc() {
    return RawArtifact.raw(
            DOC_PATH,
            MediaTypes.MARKDOWN,
            "x".getBytes(StandardCharsets.UTF_8),
            new FsProvenance(SOURCE, DOC_PATH, "hash", Instant.EPOCH))
        .withText("x");
  }

  private static Chunk docChunk(String text) {
    return new Chunk(
        "chunk-1",
        SOURCE,
        "file-1",
        DOC_PATH,
        ChunkType.DOC,
        text,
        "hash",
        5,
        1,
        3,
        null,
        new FsProvenance(SOURCE, DOC_PATH, "hash", Instant.EPOCH));
  }

  @Test
  void scoresQualifiedRefsHighAndAmbiguousNamesLow() {
    IdentifierMatchLinker linker = new IdentifierMatchLinker(resolver);
    Chunk chunk = docChunk("The com.example.Greeter greets. Also Foo helps.");

    List<LinkCandidate> candidates = linker.link(doc(), List.of(chunk));

    assertThat(candidates)
        .allSatisfy(c -> assertThat(c.type()).isEqualTo(RelationType.DESCRIBES))
        .anySatisfy(
            c -> {
              assertThat(c.toId()).isEqualTo("E:greeter");
              assertThat(c.score()).isEqualTo(0.9);
            })
        .anySatisfy(
            c -> {
              assertThat(c.toId()).isEqualTo("F1");
              assertThat(c.score()).isEqualTo(0.4);
            });
  }

  @Test
  void ignoresNonDocumentChunks() {
    IdentifierMatchLinker linker = new IdentifierMatchLinker(resolver);
    Chunk code =
        new Chunk(
            "code-1",
            SOURCE,
            "file-1",
            "Greeter.java",
            ChunkType.CODE,
            "com.example.Greeter body",
            "hash",
            5,
            1,
            3,
            "E:greeter",
            new FsProvenance(SOURCE, "Greeter.java", "hash", Instant.EPOCH));

    assertThat(linker.link(doc(), List.of(code))).isEmpty();
  }

  @Test
  void linksAReferencedFilePath() {
    PathReferenceLinker linker = new PathReferenceLinker(resolver);
    Chunk chunk = docChunk("Implementation lives in src/Greeter.java today.");

    List<LinkCandidate> candidates = linker.link(doc(), List.of(chunk));

    assertThat(candidates)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.toId()).isEqualTo("E:greeter");
              assertThat(c.score()).isEqualTo(0.85);
              assertThat(c.evidence()).isEqualTo("src/Greeter.java");
            });
  }
}
