package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Reads PDF into clean, layout-aware text (page by page) via Spring AI. */
@Component
@Order(0)
class PdfReader implements DocumentReader {

  @Override
  public boolean supports(String mediaType) {
    return MediaTypes.PDF.equals(mediaType);
  }

  @Override
  public String extractText(RawArtifact artifact) {
    var reader = new PagePdfDocumentReader(ReaderSupport.resource(artifact));
    return ReaderSupport.joinText(reader.get());
  }
}
