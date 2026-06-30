package com.knowledgehub.retrieval.infrastructure.cache;

import com.knowledgehub.knowledge.sync.application.IndexCompleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Drops cached query results when a sync changes a source's index, so a query after a sync never
 * serves answers built from now-stale data. Listening to the event keeps retrieval decoupled from
 * sync - it reacts to the published fact, it is not called by the sync flow.
 */
@Component
class CacheInvalidationListener {

  private final RetrievalCache cache;

  CacheInvalidationListener(RetrievalCache cache) {
    this.cache = cache;
  }

  @EventListener
  void onIndexCompleted(IndexCompleted event) {
    cache.invalidateAll();
  }
}
