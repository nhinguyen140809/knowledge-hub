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
  void markdownReaderKeepsHeadingsVerbatim() {
    var reader = new MarkdownReader();

    assertThat(reader.supports(MediaTypes.MARKDOWN)).isTrue();
    String source = "# Title\n\nHello world from markdown.";
    String text = reader.extractText(artifact("R.md", MediaTypes.MARKDOWN, source));

    // Verbatim: the heading marker survives so the document chunker can section on it.
    assertThat(text).isEqualTo(source);
  }

  @Test
  void tikaReaderIsTheUnconditionalFallback() {
    var reader = new TikaReader();

    assertThat(reader.supports(MediaTypes.OCTET_STREAM)).isTrue();
    assertThat(reader.supports("anything/else")).isTrue();
  }

  @Test
  void tikaReaderExtractsRichDocumentsAsMarkdown() {
    var reader = new TikaReader();
    String html = "<!DOCTYPE html><html><body><h1>Title</h1><p>Body text.</p></body></html>";

    String text = reader.extractText(artifact("doc.html", MediaTypes.OCTET_STREAM, html));

    // The heading becomes a Markdown '#' heading, so structure survives for the chunker.
    assertThat(text).contains("# Title").contains("Body text.");
  }
}
