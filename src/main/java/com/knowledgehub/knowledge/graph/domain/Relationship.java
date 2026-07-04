package com.knowledgehub.knowledge.graph.domain;

import java.util.Objects;

/**
 * A directed edge between two graph entities, ready to be written. Both ends are entity ids that
 * already exist in the graph (the targets have been resolved). A deterministic {@link RelationType}
 * must have confidence 1; an inferred one carries the heuristic confidence that decided to keep it.
 *
 * @param fromId id of the source entity
 * @param toId id of the target entity
 * @param type the relationship type (also the Neo4j relationship type)
 * @param confidence certainty in {@code [0, 1]}; 1 for deterministic types
 * @param evidence the signal this was inferred from (an identifier, keyword, or path), or {@code
 *     null} for a purely structural edge
 */
public record Relationship(
    String fromId, String toId, RelationType type, double confidence, String evidence) {

  public Relationship {
    Objects.requireNonNull(fromId, "fromId");
    Objects.requireNonNull(toId, "toId");
    Objects.requireNonNull(type, "type");
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence out of range [0,1]: " + confidence);
    }
    if (type.deterministic() && confidence != 1.0) {
      throw new IllegalArgumentException(
          "deterministic relation " + type + " must have confidence 1, was " + confidence);
    }
  }

  /** A deterministic, fully-certain relationship (structural or deep). */
  public static Relationship deterministic(String fromId, String toId, RelationType type) {
    return new Relationship(fromId, toId, type, 1.0, null);
  }
}
