package com.knowledgehub.retrieval.domain;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import java.util.Collection;
import java.util.List;

/**
 * Expands a set of seed nodes along graph relationships ({@code CALLS}, {@code CONTAINS}, {@code
 * DESCRIBES}, {@code IMPLEMENTED_BY}, ...) to surface related context a pure vector match misses -
 * the GraphRAG step. Seeds come from the semantic and keyword paths; the score reflects how close
 * the expanded node is to a seed. The {@code filter} is applied as a hard pre-filter, so traversal
 * never crosses into a disallowed source even indirectly.
 */
public interface GraphTraversalPort {

  /**
   * Returns up to {@code k} nodes reachable from the seeds, best-first by proximity.
   *
   * @param seedIds the chunk/entity ids to expand from
   * @param k maximum number of expanded results
   * @param filter source/ref/type restrictions applied as a pre-filter
   */
  List<ScoredId> expand(Collection<String> seedIds, int k, Filter filter);
}
