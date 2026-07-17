package com.knowledgehub.knowledge.analysis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.analysis.domain.AnalysisResult;
import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.analysis.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DocAnalyzerTests {

  private final DocAnalyzer analyzer = new DocAnalyzer();

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

    AnalysisResult result = analyzer.analyze(markdown(text), new ChunkConfig(512, 0));

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

    AnalysisResult result = analyzer.analyze(markdown(text), new ChunkConfig(10, 0));

    assertThat(result.chunks()).hasSizeGreaterThan(1);
    assertThat(result.chunks()).allSatisfy(c -> assertThat(c.tokenCount()).isLessThanOrEqualTo(10));
  }

  @Test
  void overlapCarriesTokensIntoTheNextChunk() {
    String text =
        """
        alpha words one two three four five.

        beta words six seven eight nine ten.

        gamma words again and again and again.
        """;

    AnalysisResult none = analyzer.analyze(markdown(text), new ChunkConfig(20, 0));
    AnalysisResult overlapping = analyzer.analyze(markdown(text), new ChunkConfig(20, 10));

    assertThat(none.chunks()).hasSizeGreaterThan(1);
    // Overlap duplicates trailing tokens, so the total text covered grows and chunk count does not
    // drop.
    assertThat(overlapping.chunks().size()).isGreaterThanOrEqualTo(none.chunks().size());
    assertThat(totalLength(overlapping)).isGreaterThan(totalLength(none));
  }

  @Test
  void tracksExactLineNumbers() {
    String text =
        """
        # Heading
        first line.

        second paragraph line three.
        """;

    AnalysisResult result = analyzer.analyze(markdown(text), new ChunkConfig(512, 0));

    // One section (single heading); the chunk spans from the heading (line 1) to the last content
    // line (line 4).
    Chunk chunk = result.chunks().get(0);
    assertThat(chunk.lineStart()).isEqualTo(1);
    assertThat(chunk.lineEnd()).isEqualTo(4);
  }

  @Test
  void assignsValidLineRangesAndDocType() {
    AnalysisResult result =
        analyzer.analyze(markdown("# Title\nbody line.\n"), new ChunkConfig(512, 0));

    Chunk chunk = result.chunks().get(0);
    assertThat(chunk.type()).isEqualTo(ChunkType.DOC);
    assertThat(chunk.lineStart()).isEqualTo(1);
    assertThat(chunk.lineEnd()).isGreaterThanOrEqualTo(chunk.lineStart());
    assertThat(chunk.entityId()).isNull();
  }

  private static int totalLength(AnalysisResult result) {
    return result.chunks().stream().mapToInt(c -> c.text().length()).sum();
  }
}
