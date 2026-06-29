package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fallback reader for any format without a dedicated reader (e.g. DOCX, ODT, HTML) via Apache Tika.
 * Lowest precedence, so a more specific reader is always preferred when one matches.
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
    var reader = new TikaDocumentReader(ReaderSupport.resource(artifact));
    return ReaderSupport.joinText(reader.get());
  }
}
