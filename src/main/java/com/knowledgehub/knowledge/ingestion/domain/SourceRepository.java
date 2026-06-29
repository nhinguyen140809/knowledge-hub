package com.knowledgehub.knowledge.ingestion.domain;

import java.util.List;
import java.util.Optional;

/** Port for persisting and looking up configured {@link Source}s. */
public interface SourceRepository {

  /** Inserts or updates the source; returns the stored instance. */
  Source save(Source source);

  /** Finds a source by id, or empty if none exists. */
  Optional<Source> findById(String sourceId);

  /** All configured sources (small, bounded list). */
  List<Source> findAll();

  /** Removes the source by id; a no-op if it does not exist. */
  void deleteById(String sourceId);
}
