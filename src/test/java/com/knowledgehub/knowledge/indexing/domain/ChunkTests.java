package com.knowledgehub.knowledge.indexing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.Provenance;
import com.knowledgehub.shared.id.Hashing;
import com.knowledgehub.shared.id.IdFactory;
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

  @Test
  void createDerivesContentHashAndStableIdsFromContent() {
    Provenance provenance = provenance();
    Chunk chunk = Chunk.create(provenance, "a.md", ChunkType.DOC, "hello", 3, 1, 4, null);

    assertThat(chunk.sourceId()).isEqualTo(provenance.sourceId());
    assertThat(chunk.contentHash()).isEqualTo(Hashing.sha256("hello"));
    assertThat(chunk.fileId()).isEqualTo(IdFactory.fileId(provenance.sourceId(), "a.md"));
    assertThat(chunk.chunkId())
        .isEqualTo(Chunk.deriveId(provenance.sourceId(), "a.md", chunk.contentHash()));
  }

  @Test
  void createIsIdempotentForSameContentAndDivergesForDifferent() {
    Chunk a = Chunk.create(provenance(), "a.md", ChunkType.DOC, "hello", 3, 1, 4, null);
    Chunk same = Chunk.create(provenance(), "a.md", ChunkType.DOC, "hello", 3, 1, 4, null);
    Chunk different = Chunk.create(provenance(), "a.md", ChunkType.DOC, "world", 3, 1, 4, null);

    assertThat(same.chunkId()).isEqualTo(a.chunkId());
    assertThat(different.chunkId()).isNotEqualTo(a.chunkId());
    // identity of the file is independent of chunk content
    assertThat(different.fileId()).isEqualTo(a.fileId());
  }
}
