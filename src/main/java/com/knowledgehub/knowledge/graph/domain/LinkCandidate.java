package com.knowledgehub.knowledge.graph.domain;

import java.util.Objects;

/**
 * A proposed cross-artifact relationship before it is accepted. A {@link CrossArtifactLinker}
 * produces candidates with a heuristic {@code score}; the linking step keeps only those at or above
 * the configured confidence threshold and turns them into {@link Relationship}s. Keeping candidates
 * separate from relationships means the heuristic and the accept/reject policy stay decoupled.
 *
 * @param fromId id of the source entity (already resolved)
 * @param toId id of the target entity (already resolved)
 * @param type the cross-artifact relationship type this candidate would create
 * @param score heuristic certainty in {@code [0, 1]}
 * @param evidence the signal the score is based on (an identifier, keyword, or path)
 */
public record LinkCandidate(
    String fromId, String toId, RelationType type, double score, String evidence) {

  public LinkCandidate {
    Objects.requireNonNull(fromId, "fromId");
    Objects.requireNonNull(toId, "toId");
    Objects.requireNonNull(type, "type");
    if (type.deterministic()) {
      throw new IllegalArgumentException("structural relation " + type + " is not a link candidate");
    }
    if (score < 0.0 || score > 1.0) {
      throw new IllegalArgumentException("score out of range [0,1]: " + score);
    }
  }

  /** The relationship this candidate becomes once accepted, carrying its score as confidence. */
  public Relationship toRelationship() {
    return new Relationship(fromId, toId, type, score, evidence);
  }
}
