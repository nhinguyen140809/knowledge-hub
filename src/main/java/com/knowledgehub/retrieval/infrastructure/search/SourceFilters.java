package com.knowledgehub.retrieval.infrastructure.search;

import com.knowledgehub.knowledge.domain.Filter;
import java.util.List;

/**
 * Pushes a {@link Filter}'s {@code allowedSources} into a Cypher query as a hard pre-filter. Every
 * read path binds {@code unrestricted} + {@code allowedSources} and guards each match with {@code
 * $unrestricted OR node.source_id IN $allowedSources}, so a disallowed source is never read.
 */
final class SourceFilters {

  private SourceFilters() {}

  /** True when the filter allows no source at all - the caller should skip the round-trip. */
  static boolean restrictedToNothing(Filter filter) {
    return !filter.isUnrestricted() && filter.allowedSources().isEmpty();
  }

  /** The allow-list to bind ({@code unrestricted} carries the "no restriction" case separately). */
  static List<String> allowedSources(Filter filter) {
    return filter.isUnrestricted() ? List.of() : List.copyOf(filter.allowedSources());
  }
}
