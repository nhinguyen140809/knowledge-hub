package com.knowledgehub.knowledge.indexing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.Provenance;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChunkTests {

  private static Provenance provenance() {
    return new FsProvenance("src", "a.md", "hash", Instant.EPOCH);
  }

  private static Chunk chunk(String text, int lineStart, int lineEnd) {
    return new Chunk(
        "cid",
        "src",
        "fid",
        "a.md",
        ChunkType.DOC,
        text,
        "hash",
        3,
        lineStart,
        lineEnd,
        null,
        provenance());
  }

  @Test
  void buildsAValidChunk() {
    Chunk chunk = chunk("hello", 1, 4);
    assertThat(chunk.text()).isEqualTo("hello");
    assertThat(chunk.entityId()).isNull();
  }

  @Test
  void rejectsBlankText() {
    assertThatThrownBy(() -> chunk("  ", 1, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
  }

  @Test
  void rejectsInvalidLineRange() {
    assertThatThrownBy(() -> chunk("x", 5, 2)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> chunk("x", 0, 1)).isInstanceOf(IllegalArgumentException.class);
  }
}
