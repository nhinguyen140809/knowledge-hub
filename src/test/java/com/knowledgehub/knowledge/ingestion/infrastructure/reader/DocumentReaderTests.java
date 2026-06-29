package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DocumentReaderTests {

  private static RawArtifact artifact(String path, String mediaType, String body) {
    return RawArtifact.raw(
        path,
        mediaType,
        body.getBytes(StandardCharsets.UTF_8),
        new FsProvenance("src", path, "hash", Instant.parse("2026-01-01T00:00:00Z")));
  }

  @Test
  void plainTextReaderReturnsExactBytes() {
    var reader = new PlainTextReader();
    String code = "class A {\n  int x;\n}\n";

    assertThat(reader.supports(MediaTypes.PLAIN_TEXT)).isTrue();
    assertThat(reader.supports(MediaTypes.MARKDOWN)).isFalse();
    assertThat(reader.extractText(artifact("A.java", MediaTypes.PLAIN_TEXT, code))).isEqualTo(code);
  }

  @Test
  void markdownReaderExtractsBodyText() {
    var reader = new MarkdownReader();

    assertThat(reader.supports(MediaTypes.MARKDOWN)).isTrue();
    String text =
        reader.extractText(
            artifact("R.md", MediaTypes.MARKDOWN, "# Title\n\nHello world from markdown."));

    // Spring AI surfaces the heading as document metadata; the body becomes the extracted text.
    assertThat(text).contains("Hello world from markdown.");
  }

  @Test
  void tikaReaderIsTheUnconditionalFallback() {
    var reader = new TikaReader();

    assertThat(reader.supports(MediaTypes.OCTET_STREAM)).isTrue();
    assertThat(reader.supports("anything/else")).isTrue();
  }
}
