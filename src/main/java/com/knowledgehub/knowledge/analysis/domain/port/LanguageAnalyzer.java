package com.knowledgehub.knowledge.analysis.domain.port;

import com.knowledgehub.knowledge.analysis.domain.AnalysisResult;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;

/**
 * Cuts an artifact into chunks (and, for code, extracts entities). A Strategy: each variant handles
 * one kind of input — one per source language ({@code JavaAnalyzer}) plus the {@code DocAnalyzer}
 * fallback; the selector injects the whole set and picks the first that {@link #supports} the
 * artifact, so adding a language is one new adapter.
 */
public interface LanguageAnalyzer {

  /** Whether this analyzer can handle the given artifact (by media type and/or path). */
  boolean supports(RawArtifact artifact);

  /**
   * Cuts the artifact into chunks honouring the config. Implementations never split an indivisible
   * unit (a function) across chunks.
   *
   * @param artifact the artifact to chunk (must carry extracted text)
   * @param config the chunking tunables
   * @return the chunks plus any extracted code entities
   */
  AnalysisResult analyze(RawArtifact artifact, ChunkConfig config);
}
