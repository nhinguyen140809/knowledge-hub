package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reads Markdown verbatim as UTF-8. The source is already Markdown, so keeping the exact bytes
 * preserves the heading markers ({@code #}) the structure-aware document chunker sections on — a
 * parse-and-re-render step would strip them into metadata and flatten the structure.
 */
@Component
@Order(0)
class MarkdownReader implements DocumentReader {

  @Override
  public boolean supports(String mediaType) {
    return MediaTypes.MARKDOWN.equals(mediaType);
  }

  @Override
  public String extractText(RawArtifact artifact) {
    return new String(artifact.content(), StandardCharsets.UTF_8);
  }
}
