package com.knowledgehub.knowledge.analysis.domain;

import java.util.Locale;

/**
 * The kind of content a {@link Chunk} holds. Carried into the vector metadata and the graph node so
 * retrieval can filter by data type (the {@code type} dimension of the search filter).
 */
public enum ChunkType {
  CODE,
  DOC;

  /** Lowercase wire form used in vector metadata and node properties ({@code code}/{@code doc}). */
  public String wireName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
