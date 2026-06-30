package com.knowledgehub.retrieval.domain;

import java.util.Objects;

/**
 * One ranked result: the id of a matched chunk or code entity, its fused relevance score, and the
 * metadata needed to cite and fetch it.
 *
 * @param id the matched chunk id or entity id
 * @param relevanceScore the fused relevance score (higher is more relevant)
 * @param metadata where the hit lives and how it was found
 */
public record Hit(String id, double relevanceScore, HitMetadata metadata) {

  public Hit {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(metadata, "metadata");
  }
}
