package com.knowledgehub.retrieval.application;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RankedResult;
import java.util.List;

/**
 * Mutable state carried through the retrieval pipeline: the query and its resolved restrictions in,
 * then the prepared keywords, each path's hits, the fused ranking, the assembled hits, and finally
 * the {@link RankedResult}. Every per-run value lives here so the stages stay stateless and the
 * search stages can fan out in parallel - each writes only its own hit list.
 *
 * <p>{@code aclFilter} carries {@code allowedSources} (the hard pre-filter pushed into every search
 * path). {@code effectiveRef} is the ref to filter by after the canonical-ref fallback has run
 * ({@code null} means no ref restriction), and {@code servedFromCanonicalRef} records whether that
 * fallback fired.
 */
class RetrievalContext {

  private final Query query;
  private final Filter aclFilter;

  private int topK;
  private String effectiveRef;
  private String typeFilter;
  private boolean servedFromCanonicalRef;

  private List<String> keywords = List.of();
  private List<ScoredId> semanticHits = List.of();
  private List<ScoredId> keywordHits = List.of();
  private List<ScoredId> graphHits = List.of();
  private List<ScoredId> fusedHits = List.of();
  private List<Hit> assembledHits = List.of();
  private RankedResult result = RankedResult.empty();

  RetrievalContext(Query query, Filter aclFilter) {
    this.query = query;
    this.aclFilter = aclFilter;
  }

  Query query() {
    return query;
  }

  Filter aclFilter() {
    return aclFilter;
  }

  int topK() {
    return topK;
  }

  void setTopK(int topK) {
    this.topK = topK;
  }

  String effectiveRef() {
    return effectiveRef;
  }

  void setEffectiveRef(String effectiveRef) {
    this.effectiveRef = effectiveRef;
  }

  String typeFilter() {
    return typeFilter;
  }

  void setTypeFilter(String typeFilter) {
    this.typeFilter = typeFilter;
  }

  boolean servedFromCanonicalRef() {
    return servedFromCanonicalRef;
  }

  void setServedFromCanonicalRef(boolean servedFromCanonicalRef) {
    this.servedFromCanonicalRef = servedFromCanonicalRef;
  }

  List<String> keywords() {
    return keywords;
  }

  void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

  List<ScoredId> semanticHits() {
    return semanticHits;
  }

  void setSemanticHits(List<ScoredId> semanticHits) {
    this.semanticHits = semanticHits;
  }

  List<ScoredId> keywordHits() {
    return keywordHits;
  }

  void setKeywordHits(List<ScoredId> keywordHits) {
    this.keywordHits = keywordHits;
  }

  List<ScoredId> graphHits() {
    return graphHits;
  }

  void setGraphHits(List<ScoredId> graphHits) {
    this.graphHits = graphHits;
  }

  List<ScoredId> fusedHits() {
    return fusedHits;
  }

  void setFusedHits(List<ScoredId> fusedHits) {
    this.fusedHits = fusedHits;
  }

  List<Hit> assembledHits() {
    return assembledHits;
  }

  void setAssembledHits(List<Hit> assembledHits) {
    this.assembledHits = assembledHits;
  }

  RankedResult result() {
    return result;
  }

  void setResult(RankedResult result) {
    this.result = result;
  }
}
