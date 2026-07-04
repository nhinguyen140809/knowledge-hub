package com.knowledgehub.knowledge.graph.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RelationshipTests {

  @Test
  void structuralRelationsAreAlwaysCertain() {
    Relationship rel = Relationship.deterministic("a", "b", RelationType.CALLS);
    assertThat(rel.confidence()).isEqualTo(1.0);
    assertThat(rel.type().deterministic()).isTrue();
  }

  @Test
  void rejectsADeterministicRelationBelowFullConfidence() {
    assertThatThrownBy(() -> new Relationship("a", "b", RelationType.IMPORTS, 0.5, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsConfidenceOutOfRange() {
    assertThatThrownBy(() -> new Relationship("a", "b", RelationType.DESCRIBES, 1.5, "Foo"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void crossArtifactRelationKeepsItsHeuristicConfidence() {
    Relationship rel = new Relationship("doc", "code", RelationType.DESCRIBES, 0.7, "Greeter");
    assertThat(rel.confidence()).isEqualTo(0.7);
    assertThat(rel.type().category()).isEqualTo(RelationCategory.CROSS_ARTIFACT);
  }
}
