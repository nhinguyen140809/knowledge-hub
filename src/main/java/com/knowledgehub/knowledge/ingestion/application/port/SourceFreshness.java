package com.knowledgehub.knowledge.ingestion.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/**
 * Read port for how fresh a source's index is — the instant it was last synced. Declared by
 * ingestion so a source view can report {@code updatedAt} without ingestion depending on the sync
 * context; the sync context, which owns freshness and already depends on ingestion, supplies the
 * implementation, so the dependency arrow keeps pointing one way (sync to ingestion).
 */
public interface SourceFreshness {

  /**
   * The last-indexed instant of each given source, keyed by source id. A source that has never been
   * synced is absent from the map rather than mapped to {@code null}, so a missing entry reads as
   * "never synced".
   *
   * @param sourceIds the sources to look up (may be empty)
   * @return last-indexed instants by source id, only for sources that have been synced at least
   *     once
   */
  Map<String, Instant> lastIndexedAt(Collection<String> sourceIds);
}
