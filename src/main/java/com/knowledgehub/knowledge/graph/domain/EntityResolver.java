package com.knowledgehub.knowledge.graph.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Turns textual references (from call sites, imports, or prose in a document) into the ids of
 * existing {@link com.knowledgehub.knowledge.indexing.domain.CodeEntity} nodes. This is what keeps
 * the graph from fragmenting: a name at a use site is mapped back to the node that declares it,
 * preferring the requesting source and widening to other sources for cross-source links.
 *
 * <p>All lookups are batched - a caller collects the references it found and resolves them in one
 * round-trip - so linking a file or document stays a couple of queries rather than one per token.
 */
public interface EntityResolver {

  /**
   * Resolves exact fully-qualified names to a single entity each (used for deterministic structural
   * relations). A name that does not resolve, or that resolves ambiguously, is simply absent from
   * the result rather than guessed.
   *
   * @param qualifiedNames the fully-qualified names to resolve
   * @param scope the lookup preference
   * @return a map from each resolved name to its entity id
   */
  Map<String, String> resolve(Collection<String> qualifiedNames, ResolutionScope scope);

  /**
   * Finds entities whose simple name matches (used by heuristic cross-artifact linking, which only
   * sees informal names in prose).
   *
   * @param simpleNames the simple names to match
   * @param scope the lookup preference
   * @return a map from each name to its matching entity ids, source-preferred first
   */
  Map<String, List<String>> findByName(Collection<String> simpleNames, ResolutionScope scope);

  /**
   * Finds entities declared in referenced file paths (used when a document points at a path).
   *
   * @param paths the file paths mentioned
   * @param scope the lookup preference
   * @return a map from each path to the ids of entities declared in it, source-preferred first
   */
  Map<String, List<String>> findByPath(Collection<String> paths, ResolutionScope scope);
}
