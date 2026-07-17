package com.knowledgehub.knowledge.ingestion.domain.port;

import com.knowledgehub.knowledge.ingestion.domain.CommitRecord;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;

/**
 * Port that reads a source's commit history, newest first. Only version-controlled source types
 * have an adapter; a filesystem source has no history and no adapter, so callers treat "no adapter
 * supports this source" as "nothing to read".
 */
public interface CommitHistoryPort {

  /** Whether this adapter can read history for the given source type. */
  boolean supports(SourceType type);

  /**
   * Reads the newest commits reachable from the source's configured ref, newest first. The walk
   * stops early at {@code sinceSha} (exclusive) so an incremental caller pays only for what is new;
   * when {@code sinceSha} is {@code null} or no longer on the branch, the walk runs to {@code
   * limit}.
   *
   * @param source the source whose history to read
   * @param sinceSha the last commit already known, or {@code null} to read up to the limit
   * @param limit maximum number of commits to return
   * @return the commits newer than {@code sinceSha}, newest first; empty when up to date
   */
  List<CommitRecord> history(Source source, String sinceSha, int limit);
}
