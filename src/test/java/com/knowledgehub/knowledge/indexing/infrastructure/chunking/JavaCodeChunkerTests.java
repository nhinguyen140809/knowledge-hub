package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkConfig;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.indexing.domain.ChunkingResult;
import com.knowledgehub.knowledge.indexing.domain.CodeEntity;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityLevel;
import com.knowledgehub.knowledge.infrastructure.lang.JavaLanguage;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JavaCodeChunkerTests {

  private final JavaCodeChunker chunker = new JavaCodeChunker(new JavaLanguage());

  private static final String SOURCE =
      """
      package com.example;

      /** A greeter. */
      public class Greeter {
        private final String name;

        public Greeter(String name) {
          this.name = name;
        }

        /** Builds a greeting. */
        public String greet() {
          String prefix = "Hello, ";
          return prefix + name;
        }
      }
      """;

  private static RawArtifact java(String path, String text) {
    return RawArtifact.raw(
            path,
            MediaTypes.PLAIN_TEXT,
            text.getBytes(StandardCharsets.UTF_8),
            new FsProvenance("src", path, "hash", Instant.EPOCH))
        .withText(text);
  }

  @Test
  void supportsOnlyJavaFiles() {
    assertThat(chunker.supports(java("Greeter.java", SOURCE))).isTrue();
    assertThat(chunker.supports(java("notes.md", "x"))).isFalse();
  }

  @Test
  void cutsOneChunkPerMethodWithoutSplittingTheBody() {
    ChunkingResult result = chunker.chunk(java("Greeter.java", SOURCE), new ChunkConfig(8, 0));

    Chunk greet =
        result.chunks().stream()
            .filter(c -> c.text().contains("String greet()"))
            .findFirst()
            .orElseThrow();
    // The full method body stays in one chunk even though it exceeds the tiny token budget.
    assertThat(greet.type()).isEqualTo(ChunkType.CODE);
    assertThat(greet.text()).contains("String prefix").contains("return prefix + name;");
    assertThat(greet.text()).contains("/** Builds a greeting. */"); // Javadoc kept with the method
    assertThat(greet.entityId()).isNotNull();
  }

  @Test
  void extractsTheEntityHierarchy() {
    ChunkingResult result = chunker.chunk(java("Greeter.java", SOURCE), new ChunkConfig(512, 0));

    CodeEntity type =
        result.codeEntities().stream()
            .filter(e -> e.level() == CodeEntityLevel.CLASS)
            .findFirst()
            .orElseThrow();
    assertThat(type.name()).isEqualTo("Greeter");
    assertThat(type.parentEntityId()).isNull();

    assertThat(result.codeEntities())
        .anySatisfy(
            e -> {
              assertThat(e.level()).isEqualTo(CodeEntityLevel.METHOD);
              assertThat(e.name()).isEqualTo("greet");
              assertThat(e.parentEntityId()).isEqualTo(type.entityId());
            });
    assertThat(result.codeEntities())
        .anySatisfy(e -> assertThat(e.level()).isEqualTo(CodeEntityLevel.CONSTRUCTOR));
    assertThat(result.codeEntities())
        .anySatisfy(
            e -> {
              assertThat(e.level()).isEqualTo(CodeEntityLevel.FIELD);
              assertThat(e.name()).isEqualTo("name");
            });
  }

  @Test
  void shellChunkHoldsClassContextWithoutMethodBodies() {
    ChunkingResult result = chunker.chunk(java("Greeter.java", SOURCE), new ChunkConfig(512, 0));

    Chunk shell =
        result.chunks().stream()
            .filter(c -> c.text().contains("class Greeter"))
            .findFirst()
            .orElseThrow();
    assertThat(shell.text()).contains("private final String name;");
    assertThat(shell.text()).doesNotContain("return prefix + name;");
  }

  @Test
  void extractsEveryVariableOfAMultiDeclaratorField() {
    String source =
        """
        package com.example;

        public class Coords {
          private int x, y, z;
        }
        """;

    ChunkingResult result = chunker.chunk(java("Coords.java", source), new ChunkConfig(512, 0));

    assertThat(result.codeEntities())
        .filteredOn(e -> e.level() == CodeEntityLevel.FIELD)
        .extracting(CodeEntity::name)
        .containsExactlyInAnyOrder("x", "y", "z");
  }

  @Test
  void throwsOnUnparseableJava() {
    assertThatThrownBy(
            () -> chunker.chunk(java("Bad.java", "this is not java {{{"), new ChunkConfig(512, 0)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
