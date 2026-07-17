package com.knowledgehub.knowledge.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.analysis.domain.AnalysisResult;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.analysis.domain.LanguageAnalyzer;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyzeStageTests {

  private static final ChunkConfig CONFIG = new ChunkConfig(512, 0);

  private static IndexingContext context() {
    return new IndexingContext(IndexingFixtures.markdownArtifact("# Title\nbody"), CONFIG);
  }

  @Test
  void usesTheFirstSupportingAnalyzer() {
    LanguageAnalyzer supporting =
        new LanguageAnalyzer() {
          @Override
          public boolean supports(RawArtifact artifact) {
            return true;
          }

          @Override
          public AnalysisResult analyze(RawArtifact artifact, ChunkConfig config) {
            return AnalysisResult.ofChunks(List.of(IndexingFixtures.docChunk("body")));
          }
        };

    IndexingContext result = new AnalyzeStage(List.of(supporting)).apply(context());

    assertThat(result.isSkipped()).isFalse();
    assertThat(result.chunks()).hasSize(1);
  }

  @Test
  void skipsWhenNoAnalyzerSupportsTheArtifact() {
    LanguageAnalyzer none =
        new LanguageAnalyzer() {
          @Override
          public boolean supports(RawArtifact artifact) {
            return false;
          }

          @Override
          public AnalysisResult analyze(RawArtifact artifact, ChunkConfig config) {
            throw new AssertionError("must not be called");
          }
        };

    IndexingContext result = new AnalyzeStage(List.of(none)).apply(context());

    assertThat(result.isSkipped()).isTrue();
    assertThat(result.chunks()).isEmpty();
  }

  @Test
  void skipsWhenChunkingThrows() {
    LanguageAnalyzer failing =
        new LanguageAnalyzer() {
          @Override
          public boolean supports(RawArtifact artifact) {
            return true;
          }

          @Override
          public AnalysisResult analyze(RawArtifact artifact, ChunkConfig config) {
            throw new IllegalArgumentException("bad input");
          }
        };

    IndexingContext result = new AnalyzeStage(List.of(failing)).apply(context());

    assertThat(result.isSkipped()).isTrue();
    assertThat(result.skipReason()).contains("chunking failed");
  }
}
