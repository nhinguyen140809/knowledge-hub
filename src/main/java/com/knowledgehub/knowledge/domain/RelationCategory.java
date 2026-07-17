package com.knowledgehub.knowledge.domain;

/**
 * The kind of knowledge a relationship encodes, which decides how it is produced and trusted.
 *
 * <ul>
 *   <li>{@link #STRUCTURAL} — read straight from the syntax (calls, imports, inheritance). A fact,
 *       so confidence is always 1.
 *   <li>{@link #DEEP} — finer syntactic relations (instantiation, field access, type use). Also
 *       deterministic, extracted alongside the structural pass.
 *   <li>{@link #CROSS_ARTIFACT} — inferred across artifacts (doc mentions code, requirement matches
 *       a test). Heuristic, so every link carries a confidence and is kept only above a threshold.
 * </ul>
 */
public enum RelationCategory {
  STRUCTURAL,
  DEEP,
  CROSS_ARTIFACT
}
