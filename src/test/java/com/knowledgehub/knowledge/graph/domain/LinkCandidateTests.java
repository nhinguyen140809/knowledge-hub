package com.knowledgehub.knowledge.graph.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LinkCandidateTests {

  @Test
  void becomesARelationshipCarryingItsScoreAsConfidence() {
    LinkCandidate candidate =
        new LinkCandidate("doc", "code", RelationType.DESCRIBES, 0.8, "Greeter");

    Relationship rel = candidate.toRelationship();

    assertThat(rel.type()).isEqualTo(RelationType.DESCRIBES);
    assertThat(rel.confidence()).isEqualTo(0.8);
    assertThat(rel.evidence()).isEqualTo("Greeter");
  }

  @Test
  void rejectsAStructuralType() {
    assertThatThrownBy(() -> new LinkCandidate("a", "b", RelationType.CALLS, 0.8, "x"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
