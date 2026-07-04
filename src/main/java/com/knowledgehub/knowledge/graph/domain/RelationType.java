package com.knowledgehub.knowledge.graph.domain;

/**
 * Every relationship type the knowledge graph can hold, grouped by {@link RelationCategory}. The
 * enum name is the Neo4j relationship type written to the graph (e.g. {@code CALLS}). A {@code
 * deterministic} type is read directly from syntax and is always stored with confidence 1; a
 * non-deterministic type is inferred and must carry a heuristic confidence.
 *
 * <p>This is the full graph vocabulary. Not every type has a producer yet: {@code TESTS} needs a
 * signal beyond single-file syntax, and {@code MODIFIES} (commit to changed code), {@code CONSUMES}
 * (an API call across services) and {@code LINKS_TO} (document to document) depend on node kinds
 * and signals sourced from other ingestion paths. Defining them up front keeps the schema and
 * traversal stable regardless of which producers exist.
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

  // --- Deeper structural relations: deterministic, extracted alongside the structural pass ---
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

  /**
   * Whether the linking step owns edges of this type and may drop and rebuild them. {@code
   * CONTAINS} and {@code DECLARES} are the entity hierarchy, written by indexing together with the
   * entities themselves; every other type is produced by linking.
   */
  public boolean linkerOwned() {
    return this != CONTAINS && this != DECLARES;
  }
}
