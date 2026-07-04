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

  /** The caller's free-text query; set at construction, read by every search stage. */
  private final Query query;

  /** Allowed sources of the caller; set at construction, pushed into every store query. */
  private final Filter aclFilter;

  /** Result-size cap; resolved from params or config by the service before the stages run. */
  private int topK;

  /**
   * Ref to filter by after the canonical-ref fallback; set by the service, null = no restriction.
   */
  private String effectiveRef;

  /** Data-type restriction from the params; set by the service, null = any type. */
  private String typeFilter;

  /** Whether the canonical-ref fallback fired; set by the service, reported in the result. */
  private boolean servedFromCanonicalRef;

  /** Query tokens for the keyword path; set by PrepareQueryStage. */
  private List<String> keywords = List.of();

  /** Vector-store matches; set by SemanticSearchStage (parallel with the keyword path). */
  private List<ScoredId> semanticHits = List.of();

  /** Full-text matches; set by KeywordSearchStage (parallel with the semantic path). */
  private List<ScoredId> keywordHits = List.of();

  /** Graph-expanded matches seeded from the two lists above; set by GraphTraversalStage. */
  private List<ScoredId> graphHits = List.of();

  /** The three lists merged into one ranking; set by RrfFusionStage. */
  private List<ScoredId> fusedHits = List.of();

  /** Fused ids loaded into full hits with metadata; set by AssembleResultStage. */
  private List<Hit> assembledHits = List.of();

  /** The final ranked answer after the last ACL/type/ref cut; set by AclFilterStage. */
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
