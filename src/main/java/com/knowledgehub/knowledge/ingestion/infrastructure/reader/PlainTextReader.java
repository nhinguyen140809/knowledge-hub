package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reads code and plain-text files verbatim as UTF-8 — no parsing, so the exact bytes survive for
 * the downstream AST/structure-aware chunking.
 */
@Component
@Order(0)
class PlainTextReader implements DocumentReader {

  @Override
  public boolean supports(String mediaType) {
    return MediaTypes.PLAIN_TEXT.equals(mediaType);
  }

  @Override
  public String extractText(RawArtifact artifact) {
    return new String(artifact.content(), StandardCharsets.UTF_8);
  }
}
