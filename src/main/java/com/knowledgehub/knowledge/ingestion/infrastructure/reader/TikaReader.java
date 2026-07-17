package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.port.DocumentReader;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fallback reader for any format without a dedicated reader (e.g. DOCX, ODT, HTML) via Apache Tika,
 * emitting Markdown so a document's headings survive for structure-aware chunking. Lowest
 * precedence, so a more specific reader is always preferred when one matches.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class TikaReader implements DocumentReader {

  @Override
  public boolean supports(String mediaType) {
    return true; // fallback: handles whatever no other reader claimed
  }

  @Override
  public String extractText(RawArtifact artifact) {
    return ReaderSupport.toMarkdown(artifact);
  }
}
