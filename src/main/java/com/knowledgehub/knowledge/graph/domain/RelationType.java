package com.knowledgehub.knowledge.graph.domain;

/**
 * Every relationship type the knowledge graph can hold, grouped by {@link RelationCategory}. The
 * enum name is the Neo4j relationship type written to the graph (e.g. {@code CALLS}). A {@code
 * deterministic} type is read directly from syntax and is always stored with confidence 1; a
 * non-deterministic type is inferred and must carry a heuristic confidence.
 */
public enum RelationType {

  // --- Structural: read directly from the AST (deterministic, confidence = 1) ---
  CONTAINS(RelationCategory.STRUCTURAL),
  DECLARES(RelationCategory.STRUCTURAL),
  CALLS(RelationCategory.STRUCTURAL),
  IMPORTS(RelationCategory.STRUCTURAL),
  EXTENDS(RelationCategory.STRUCTURAL),
  IMPLEMENTS(RelationCategory.STRUCTURAL),
  OVERRIDES(RelationCategory.STRUCTURAL),

  // --- Deeper structural relations: optional, deterministic, behind a config switch ---
  INSTANTIATES(RelationCategory.DEEP),
  READS(RelationCategory.DEEP),
  WRITES(RelationCategory.DEEP),
  REFERENCES(RelationCategory.DEEP),
  HAS_TYPE(RelationCategory.DEEP),
  ANNOTATED_WITH(RelationCategory.DEEP),
  THROWS(RelationCategory.DEEP),
  TESTS(RelationCategory.DEEP),

  // --- Cross-artifact: inferred, heuristic, confidence required ---
  DESCRIBES(RelationCategory.CROSS_ARTIFACT),
  IMPLEMENTED_BY(RelationCategory.CROSS_ARTIFACT),
  VERIFIED_BY(RelationCategory.CROSS_ARTIFACT),
  MODIFIES(RelationCategory.CROSS_ARTIFACT),
  CONSUMES(RelationCategory.CROSS_ARTIFACT),
  LINKS_TO(RelationCategory.CROSS_ARTIFACT);

  private final RelationCategory category;

  RelationType(RelationCategory category) {
    this.category = category;
  }

  public RelationCategory category() {
    return category;
  }

  /** Whether this type is read straight from syntax (so it is always certain). */
  public boolean deterministic() {
    return category != RelationCategory.CROSS_ARTIFACT;
  }
}
