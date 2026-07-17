package com.knowledgehub.knowledge.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.analysis.domain.Chunker;
import com.knowledgehub.knowledge.analysis.domain.ChunkingResult;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkStageTests {

  private static final ChunkConfig CONFIG = new ChunkConfig(512, 0);

  private static IndexingContext context() {
    return new IndexingContext(IndexingFixtures.markdownArtifact("# Title\nbody"), CONFIG);
  }

  @Test
  void usesTheFirstSupportingChunker() {
    Chunker supporting =
        new Chunker() {
          @Override
          public boolean supports(RawArtifact artifact) {
            return true;
          }

          @Override
          public ChunkingResult chunk(RawArtifact artifact, ChunkConfig config) {
            return ChunkingResult.ofChunks(List.of(IndexingFixtures.docChunk("body")));
          }
        };

    IndexingContext result = new ChunkStage(List.of(supporting)).apply(context());

    assertThat(result.isSkipped()).isFalse();
    assertThat(result.chunks()).hasSize(1);
  }

  @Test
  void skipsWhenNoChunkerSupportsTheArtifact() {
    Chunker none =
        new Chunker() {
          @Override
          public boolean supports(RawArtifact artifact) {
            return false;
          }

          @Override
          public ChunkingResult chunk(RawArtifact artifact, ChunkConfig config) {
            throw new AssertionError("must not be called");
          }
        };

    IndexingContext result = new ChunkStage(List.of(none)).apply(context());

    assertThat(result.isSkipped()).isTrue();
    assertThat(result.chunks()).isEmpty();
  }

  @Test
  void skipsWhenChunkingThrows() {
    Chunker failing =
        new Chunker() {
          @Override
          public boolean supports(RawArtifact artifact) {
            return true;
          }

          @Override
          public ChunkingResult chunk(RawArtifact artifact, ChunkConfig config) {
            throw new IllegalArgumentException("bad input");
          }
        };

    IndexingContext result = new ChunkStage(List.of(failing)).apply(context());

    assertThat(result.isSkipped()).isTrue();
    assertThat(result.skipReason()).contains("chunking failed");
  }
}
