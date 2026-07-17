package com.knowledgehub.retrieval.domain.port;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.retrieval.domain.HitMetadata;
import java.util.Collection;
import java.util.Map;

/**
 * Read side of the graph used to finish a query: load the metadata for the surviving hits, and tell
 * whether a requested ref was ever indexed (for the canonical-ref fallback). Both honour {@code
 * filter} so a disallowed source never leaks through assembly either.
 */
public interface RetrievalReadPort {

  /**
   * Loads metadata for the given chunk/entity ids. Ids that do not exist (or are filtered out) are
   * simply absent from the result.
   *
   * @param ids the hit ids to resolve
   * @param filter source restriction applied to the load
   * @return a map from id to its metadata
   */
  Map<String, HitMetadata> loadMetadata(Collection<String> ids, Filter filter);

  /**
   * Whether any content is indexed at the given ref within the readable sources.
   *
   * @param ref the version/branch asked for
   * @param filter source restriction applied to the check
   * @return true if the ref is indexed and can be served directly
   */
  boolean refIndexed(String ref, Filter filter);
}
