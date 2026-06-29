package com.knowledgehub.knowledge.graph.domain;

import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;

/**
 * Reads deterministic structural relationships out of a parsed source file (calls, imports,
 * inheritance). One implementation per language, picked by {@link #supports(RawArtifact)} — adding a
 * language is a new implementation, never a change here. Targets are already resolved to entity ids,
 * so the returned relationships are ready to write with confidence 1.
 */
public interface StructuralExtractor {

  /** Whether this extractor understands the artifact's language. */
  boolean supports(RawArtifact artifact);

  /**
   * Extracts the structural relationships declared by the artifact.
   *
   * @param artifact the source file to read
   * @return the resolved structural relationships; empty when none are found
   */
  List<Relationship> extract(RawArtifact artifact);
}
