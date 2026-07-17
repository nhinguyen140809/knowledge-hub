package com.knowledgehub.knowledge.ingestion.domain.port;

import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;

/**
 * Port that extracts clean text from a raw artifact of a given media type. Each format has its own
 * adapter (Markdown, PDF, a Tika fallback); the application picks the first whose {@link
 * #supports(String)} matches. Adding a format is one new adapter, with no change elsewhere.
 */
public interface DocumentReader {

  /** Whether this reader handles the given media type (e.g. {@code text/markdown}). */
  boolean supports(String mediaType);

  /**
   * Extracts the readable text from the artifact's content.
   *
   * @param artifact the raw artifact (its {@code content} bytes are read)
   * @return the extracted text (may be empty, never {@code null})
   */
  String extractText(RawArtifact artifact);
}
