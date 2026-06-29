package com.knowledgehub.knowledge.indexing.domain;

/**
 * Granularity of a {@link CodeEntity} extracted from source. Coarser levels (package, project) are
 * derived when relationships are linked; this phase produces the levels an AST gives directly.
 */
public enum CodeEntityLevel {
  CLASS,
  INTERFACE,
  ENUM,
  METHOD,
  CONSTRUCTOR,
  FIELD
}
