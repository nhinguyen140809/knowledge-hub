package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkConfig;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.indexing.domain.ChunkingResult;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DocChunkerTests {

  private final DocChunker chunker = new DocChunker();

  private static RawArtifact markdown(String text) {
    return RawArtifact.raw(
            "doc.md",
            MediaTypes.MARKDOWN,
            text.getBytes(StandardCharsets.UTF_8),
            new FsProvenance("src", "doc.md", "hash", Instant.EPOCH))
        .withText(text);
  }

  @Test
  void startsANewChunkAtEachHeading() {
    String text =
        """
        # First
        alpha paragraph one.

        # Second
        beta paragraph two.
        """;

    ChunkingResult result = chunker.chunk(markdown(text), new ChunkConfig(512, 0));

    assertThat(result.chunks()).hasSize(2);
    assertThat(result.chunks().get(0).text()).startsWith("# First");
    assertThat(result.chunks().get(1).text()).startsWith("# Second");
    assertThat(result.codeEntities()).isEmpty();
  }

  @Test
  void packsParagraphsUpToTheTokenBudget() {
    // Each paragraph ~ a handful of tokens; a tiny budget forces several chunks.
    String text =
        """
        para one has some words here.

        para two has some words here.

        para three has some words here.
        """;

    ChunkingResult result = chunker.chunk(markdown(text), new ChunkConfig(10, 0));

    assertThat(result.chunks()).hasSizeGreaterThan(1);
    assertThat(result.chunks()).allSatisfy(c -> assertThat(c.tokenCount()).isLessThanOrEqualTo(10));
  }

  @Test
  void overlapRepeatsTrailingBlockInNextChunk() {
    String text =
        """
        aaaa bbbb.

        cccc dddd.

        eeee ffff.
        """;

    ChunkingResult withOverlap = chunker.chunk(markdown(text), new ChunkConfig(8, 4));

    assertThat(withOverlap.chunks()).hasSizeGreaterThan(1);
    // The carried block makes a paragraph appear in two adjacent chunks.
    long timesMiddleAppears =
        withOverlap.chunks().stream().filter(c -> c.text().contains("cccc dddd.")).count();
    assertThat(timesMiddleAppears).isGreaterThanOrEqualTo(2);
  }

  @Test
  void assignsValidLineRangesAndDocType() {
    ChunkingResult result =
        chunker.chunk(markdown("# Title\nbody line.\n"), new ChunkConfig(512, 0));

    Chunk chunk = result.chunks().get(0);
    assertThat(chunk.type()).isEqualTo(ChunkType.DOC);
    assertThat(chunk.lineStart()).isEqualTo(1);
    assertThat(chunk.lineEnd()).isGreaterThanOrEqualTo(chunk.lineStart());
    assertThat(chunk.entityId()).isNull();
  }
}
