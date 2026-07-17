package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reads PDF into Markdown via Apache Tika + Flexmark. A tagged PDF surfaces its heading structure
 * as Markdown headings for structure-aware chunking; an untagged PDF (no semantic heading tags —
 * the common case) yields paragraph text the analyzer still splits by size.
 */
@Component
@Order(0)
class PdfReader implements DocumentReader {

  @Override
  public boolean supports(String mediaType) {
    return MediaTypes.PDF.equals(mediaType);
  }

  @Override
  public String extractText(RawArtifact artifact) {
    return ReaderSupport.toMarkdown(artifact);
  }
}
