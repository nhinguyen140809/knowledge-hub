package com.knowledgehub.knowledge.sync.domain;

import com.knowledgehub.knowledge.ingestion.domain.Source;

/**
 * Works out what changed in a source since it was last indexed. The implementation compares the
 * source's current files against the state already stored (content hashes per file), so it detects
 * additions, modifications and deletions without re-embedding anything itself.
 */
public interface SourceDiffer {

  /** Whether this differ handles the given source. */
  boolean supports(Source source);

  /**
   * Computes the change set for a source against its indexed state.
   *
   * @param source the source to diff
   * @return the files added, modified and deleted since the last index
   */
  ChangeSet diff(Source source);
}
