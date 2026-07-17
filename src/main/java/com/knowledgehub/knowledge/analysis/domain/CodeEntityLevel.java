package com.knowledgehub.knowledge.analysis.domain;

/**
 * Granularity of a {@link CodeEntity} extracted from source. These are the levels an AST provides
 * directly; coarser levels (package, project) are derived elsewhere from entity relationships.
 */
public enum CodeEntityLevel {
  CLASS,
  INTERFACE,
  ENUM,
  METHOD,
  CONSTRUCTOR,
  FIELD
}
