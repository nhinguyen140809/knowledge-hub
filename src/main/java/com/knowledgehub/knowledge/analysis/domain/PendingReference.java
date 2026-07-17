package com.knowledgehub.knowledge.analysis.domain;

import com.knowledgehub.knowledge.domain.RelationType;
import java.util.Objects;

/**
 * A relationship the analyzer read from syntax but could not finish: the target is named, not yet
 * resolved to a stored entity id. Produced at analysis time (one parse per artifact) and carried
 * through the indexing pipeline to the linking step, which resolves every pending reference in one
 * batched lookup after the artifact's nodes are stored — a name that does not resolve to an indexed
 * entity is dropped rather than guessed.
 *
 * @param fromId id of the already-derived entity the edge starts from (captured during the walk)
 * @param targetName the referenced name as the source spells it (for Java, a fully-qualified name)
 * @param relationType the relationship type the resolved edge will carry
 */
public record PendingReference(String fromId, String targetName, RelationType relationType) {

  public PendingReference {
    Objects.requireNonNull(fromId, "fromId");
    Objects.requireNonNull(targetName, "targetName");
    Objects.requireNonNull(relationType, "relationType");
  }
}
