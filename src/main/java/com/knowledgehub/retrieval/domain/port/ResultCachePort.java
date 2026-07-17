package com.knowledgehub.retrieval.domain.port;

import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RankedResult;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Read-through cache of query results keyed per principal's allow-list, so a repeated query can be
 * answered without re-running the retrieval pipeline. How results are stored and expired is the
 * adapter's business; the contract only promises that {@code allowedSources} is part of the key, so
 * one principal is never served another's cached answer.
 */
public interface ResultCachePort {

  /**
   * Returns the cached result for the query under {@code allowedSources}, computing and storing it
   * on a miss.
   *
   * @param query the query
   * @param allowedSources the readable sources ({@code null} when unrestricted) - part of the key
   * @param compute runs the pipeline on a miss
   */
  RankedResult get(Query query, Set<String> allowedSources, Supplier<RankedResult> compute);
}
