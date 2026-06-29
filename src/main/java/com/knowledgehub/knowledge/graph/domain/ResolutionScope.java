package com.knowledgehub.knowledge.graph.domain;

import java.util.Objects;

/**
 * Where an {@link EntityResolver} should look when turning a reference into an entity. Resolution
 * prefers the requesting source, then widens to any other source so references can cross source
 * boundaries within the same product (a doc in one source pointing at code in another).
 *
 * @param sourceId the source the reference was found in (preferred match)
 */
public record ResolutionScope(String sourceId) {

  public ResolutionScope {
    Objects.requireNonNull(sourceId, "sourceId");
  }
}
