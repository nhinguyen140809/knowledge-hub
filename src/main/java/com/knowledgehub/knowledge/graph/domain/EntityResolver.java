package com.knowledgehub.knowledge.graph.domain;

import java.util.List;
import java.util.Optional;

/**
 * Turns a textual reference (from a call site, an import, or prose in a document) into the id of an
 * existing {@link com.knowledgehub.knowledge.indexing.domain.CodeEntity} in the graph. This is what
 * keeps the graph from fragmenting: a name at a use site is mapped back to the one node that
 * declares it, preferring the requesting source and widening to other sources for cross-source
 * links.
 */
public interface EntityResolver {

  /**
   * Resolves an exact fully-qualified name to a single entity (used for deterministic structural
   * relations).
   *
   * @param qualifiedName the fully-qualified name to resolve
   * @param scope the lookup preference
   * @return the entity id, or empty when nothing (or nothing unambiguous) matches
   */
  Optional<String> resolve(String qualifiedName, ResolutionScope scope);

  /**
   * Finds entities whose simple name matches (used by heuristic cross-artifact linking, which only
   * sees informal names in prose).
   *
   * @param simpleName the simple name to match
   * @param scope the lookup preference
   * @return ids of matching entities, source-preferred first; empty when none match
   */
  List<String> findByName(String simpleName, ResolutionScope scope);

  /**
   * Finds entities declared in a referenced file path (used when a document points at a path).
   *
   * @param path the file path mentioned
   * @param scope the lookup preference
   * @return ids of entities declared in that file, source-preferred first; empty when none match
   */
  List<String> findByPath(String path, ResolutionScope scope);
}
