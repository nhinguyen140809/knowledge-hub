package com.knowledgehub.knowledge.graph.infrastructure.link;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.analysis.domain.ChunkType;
import com.knowledgehub.knowledge.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.graph.domain.port.EntityResolver;
import com.knowledgehub.knowledge.infrastructure.lang.JavaLanguage;
import com.knowledgehub.knowledge.infrastructure.lang.SourceLanguages;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CrossArtifactLinkerTests {

  private static final SourceLanguages LANGUAGES = new SourceLanguages(List.of(new JavaLanguage()));

  private static final String SOURCE = "src";
  private static final String DOC_PATH = "README.md";

  private static final Map<String, String> QUALIFIED =
      Map.of("com.example.Greeter", "E:greeter", "com.example.GreeterTest", "E:greeterTest");
  private static final Map<String, List<String>> BY_NAME =
      Map.of(
          "Greeter",
          List.of("E:greeter"),
          "Foo",
          List.of("F1", "F2"),
          "CodeChunker",
          List.of("E:analyzer"),
          "Chunk",
          List.of("E:chunk"));
  private static final Map<String, List<String>> BY_PATH =
      Map.of(
          "src/Greeter.java",
          List.of("E:greeter"),
          "src/test/java/GreeterTest.java",
          List.of("E:greeterTest"));

  private final EntityResolver resolver =
      new EntityResolver() {
        @Override
        public Map<String, String> resolve(Collection<String> names, ResolutionScope scope) {
          return filter(names, QUALIFIED);
        }

        @Override
        public Map<String, List<String>> findByName(
            Collection<String> names, ResolutionScope scope) {
          return filter(names, BY_NAME);
        }

        @Override
        public Map<String, List<String>> findByPath(
            Collection<String> paths, ResolutionScope scope) {
          return filter(paths, BY_PATH);
        }
      };

  /** Returns only the catalogue entries whose key was actually asked for. */
  private static <V> Map<String, V> filter(Collection<String> keys, Map<String, V> catalogue) {
    Map<String, V> out = new HashMap<>();
    for (String key : keys) {
      if (catalogue.containsKey(key)) {
        out.put(key, catalogue.get(key));
      }
    }
    return out;
  }

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
    IdentifierMatchLinker linker = new IdentifierMatchLinker(resolver, LANGUAGES);
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
    IdentifierMatchLinker linker = new IdentifierMatchLinker(resolver, LANGUAGES);
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
  void scoresCompoundNameHighButSingleWordNameLow() {
    IdentifierMatchLinker linker = new IdentifierMatchLinker(resolver, LANGUAGES);
    Chunk chunk = docChunk("The CodeChunker splits a Chunk into pieces.");

    List<LinkCandidate> candidates = linker.link(doc(), List.of(chunk));

    assertThat(candidates)
        .anySatisfy(
            c -> {
              assertThat(c.toId()).isEqualTo("E:analyzer"); // compound, unique -> strong
              assertThat(c.score()).isEqualTo(0.7);
            })
        .anySatisfy(
            c -> {
              assertThat(c.toId()).isEqualTo("E:chunk"); // single word -> weak, dropped by default
              assertThat(c.score()).isEqualTo(0.4);
            });
  }

  @Test
  void linksRequirementToItsImplementationAndItsTest() {
    RequirementCodeLinker linker = new RequirementCodeLinker(resolver, LANGUAGES);
    Chunk chunk =
        docChunk(
            "FR-3 is implemented by com.example.Greeter and verified by"
                + " src/test/java/GreeterTest.java.");

    List<LinkCandidate> candidates = linker.link(doc(), List.of(chunk));

    assertThat(candidates)
        .allSatisfy(c -> assertThat(c.score()).isEqualTo(0.85))
        .anySatisfy(
            c -> {
              assertThat(c.type()).isEqualTo(RelationType.IMPLEMENTED_BY);
              assertThat(c.toId()).isEqualTo("E:greeter");
            })
        .anySatisfy(
            c -> {
              assertThat(c.type()).isEqualTo(RelationType.VERIFIED_BY);
              assertThat(c.toId()).isEqualTo("E:greeterTest");
            });
  }

  @Test
  void ignoresDocumentChunksWithoutARequirementId() {
    RequirementCodeLinker linker = new RequirementCodeLinker(resolver, LANGUAGES);
    Chunk chunk = docChunk("The class com.example.Greeter greets people.");

    assertThat(linker.link(doc(), List.of(chunk))).isEmpty();
  }

  @Test
  void linksAReferencedFilePath() {
    PathReferenceLinker linker = new PathReferenceLinker(resolver, LANGUAGES);
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
