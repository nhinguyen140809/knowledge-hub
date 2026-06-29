package com.knowledgehub.knowledge.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaTypesTests {

  @Test
  void detectsMarkdownPdfTextAndFallback() {
    assertThat(MediaTypes.fromPath("docs/README.md")).isEqualTo(MediaTypes.MARKDOWN);
    assertThat(MediaTypes.fromPath("a/b.markdown")).isEqualTo(MediaTypes.MARKDOWN);
    assertThat(MediaTypes.fromPath("spec.PDF")).isEqualTo(MediaTypes.PDF);
    assertThat(MediaTypes.fromPath("src/Main.java")).isEqualTo(MediaTypes.PLAIN_TEXT);
    assertThat(MediaTypes.fromPath("report.docx")).isEqualTo(MediaTypes.OCTET_STREAM);
  }

  @Test
  void fileWithoutExtensionFallsBack() {
    assertThat(MediaTypes.fromPath("Makefile")).isEqualTo(MediaTypes.OCTET_STREAM);
    assertThat(MediaTypes.fromPath("dir.with.dot/LICENSE")).isEqualTo(MediaTypes.OCTET_STREAM);
  }
}
