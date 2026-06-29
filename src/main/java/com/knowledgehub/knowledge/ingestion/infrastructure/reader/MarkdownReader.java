package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Reads Markdown into clean text, preserving heading/paragraph structure, via Spring AI. */
@Component
@Order(0)
class MarkdownReader implements DocumentReader {

  @Override
  public boolean supports(String mediaType) {
    return MediaTypes.MARKDOWN.equals(mediaType);
  }

  @Override
  public String extractText(RawArtifact artifact) {
    var reader =
        new MarkdownDocumentReader(
            ReaderSupport.resource(artifact), MarkdownDocumentReaderConfig.builder().build());
    return ReaderSupport.joinText(reader.get());
  }
}
