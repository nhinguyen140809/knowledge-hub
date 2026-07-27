package com.knowledgehub.system.domain.port;

/** Reports the scale of the vector store's collection. */
public interface VectorStoreMetrics {

  /** Points currently in the collection. */
  long pointCount();
}
