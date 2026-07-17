package com.knowledgehub.knowledge.sync.domain.port;

import com.knowledgehub.knowledge.sync.domain.FreshnessInfo;
import java.util.Optional;

/**
 * Stores and reads the last-indexed state per source - when it was synced and at which commit. The
 * differ reads it to find where the next sync starts; the status endpoint reads it to report
 * freshness.
 */
public interface FreshnessRepository {

  /** The recorded freshness for a source, or empty if it has never been synced. */
  Optional<FreshnessInfo> find(String sourceId);

  /** Records the source's freshness after a sync (idempotent upsert by source id). */
  void save(FreshnessInfo freshness);

  /** Removes a source's freshness record (on source deletion). */
  void deleteBySource(String sourceId);
}
