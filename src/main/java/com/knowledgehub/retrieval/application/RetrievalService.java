package com.knowledgehub.retrieval.application;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.QueryParams;
import com.knowledgehub.retrieval.domain.RankedResult;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import com.knowledgehub.shared.config.AppProperties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Answers a free-text query by running the hybrid retrieval pipeline: prepare the query, search the
 * semantic and keyword paths in parallel, expand the graph from their seeds, fuse the three, then
 * assemble and filter the result. Results are cached per {@code (query, allowedSources)} so a
 * repeat returns without touching the stores.
 *
 * <p>The service is only orchestration; every step is a stage with its own port. A failing search
 * path degrades to empty and is logged rather than failing the whole query, and the {@code
 * allowedSources} pre-filter is threaded into every path from the start ({@code null} means
 * unrestricted — no ACL restriction).
 */
@Service
public class RetrievalService {

  private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

  private final PrepareQueryStage prepareStage;
  private final SemanticSearchStage semanticStage;
  private final KeywordSearchStage keywordStage;
  private final GraphTraversalStage graphStage;
  private final RrfFusionStage fusionStage;
  private final AssembleResultStage assembleStage;
  private final AclFilterStage aclFilterStage;
  private final RetrievalReadPort reader;
  private final RetrievalCache cache;
  private final Executor executor;
  private final int defaultTopK;

  public RetrievalService(
      PrepareQueryStage prepareStage,
      SemanticSearchStage semanticStage,
      KeywordSearchStage keywordStage,
      GraphTraversalStage graphStage,
      RrfFusionStage fusionStage,
      AssembleResultStage assembleStage,
      AclFilterStage aclFilterStage,
      RetrievalReadPort reader,
      RetrievalCache cache,
      AppProperties properties,
      @Qualifier("retrievalExecutor") Executor executor) {
    this.prepareStage = prepareStage;
    this.semanticStage = semanticStage;
    this.keywordStage = keywordStage;
    this.graphStage = graphStage;
    this.fusionStage = fusionStage;
    this.assembleStage = assembleStage;
    this.aclFilterStage = aclFilterStage;
    this.reader = reader;
    this.cache = cache;
    this.executor = executor;
    this.defaultTopK = properties.retrieval().topK();
  }

  /**
   * Retrieves the ranked hits for a query within the readable sources.
   *
   * @param query the free-text query and its parameters
   * @param allowedSources the sources the caller may read, or {@code null} for unrestricted (no ACL
   *     restriction)
   * @return the ranked result, possibly served from the canonical ref
   */
  public RankedResult retrieve(Query query, Set<String> allowedSources) {
    long startNanos = System.nanoTime();
    RankedResult result =
        cache.get(query, allowedSources, () -> runPipeline(query, allowedSources));
    // Log the query as metadata only — never the query text, which may be sensitive.
    log.info(
        "Query served: type={} ref={} hits={} in {} ms",
        query.params().type(),
        query.params().ref(),
        result.hits().size(),
        (System.nanoTime() - startNanos) / 1_000_000);
    return result;
  }

  private RankedResult runPipeline(Query query, Set<String> allowedSources) {
    Filter aclFilter = effectiveFilter(query.params().sourceId(), allowedSources);
    RetrievalContext context = new RetrievalContext(query, aclFilter);
    resolveParams(context);

    timed("PrepareQueryStage", () -> prepareStage.apply(context));

    // The semantic and keyword paths are independent, so fan them out in parallel; each degrades to
    // empty on failure rather than failing the query.
    CompletableFuture<Void> semantic =
        CompletableFuture.runAsync(
            () -> degrade("semantic", () -> semanticStage.apply(context)), executor);
    CompletableFuture<Void> keyword =
        CompletableFuture.runAsync(
            () -> degrade("keyword", () -> keywordStage.apply(context)), executor);
    CompletableFuture.allOf(semantic, keyword).join();

    // Graph expansion seeds from the first two paths' hits, so it runs after them.
    degrade("graph", () -> graphStage.apply(context));

    timed("RrfFusionStage", () -> fusionStage.apply(context));
    timed("AssembleResultStage", () -> assembleStage.apply(context));
    timed("AclFilterStage", () -> aclFilterStage.apply(context));
    return context.result();
  }

  /**
   * The source scope pushed into every search path: the caller's readable set, narrowed to the
   * single requested source when one is given. Requesting a source outside the readable set yields
   * the empty scope — an empty result, never an error that would reveal whether the source exists.
   */
  private static Filter effectiveFilter(String sourceId, Set<String> allowedSources) {
    if (sourceId == null) {
      return allowedSources == null ? Filter.unrestricted() : Filter.ofSources(allowedSources);
    }
    if (allowedSources == null || allowedSources.contains(sourceId)) {
      return Filter.ofSources(Set.of(sourceId));
    }
    return Filter.ofSources(Set.of());
  }

  /** Resolves top-k, the type filter, and the canonical-ref fallback before searching. */
  private void resolveParams(RetrievalContext context) {
    QueryParams params = context.query().params();
    context.setTopK(params.topK() != null ? params.topK() : defaultTopK);
    context.setTypeFilter(params.type());

    String ref = params.ref();
    if (ref != null && !reader.refIndexed(ref, context.aclFilter())) {
      context.setServedFromCanonicalRef(true);
      context.setEffectiveRef(null); // serve the canonical ref instead of the unindexed one
    } else {
      context.setEffectiveRef(ref);
    }
  }

  private static void timed(String stage, Runnable step) {
    long startNanos = System.nanoTime();
    step.run();
    log.debug("Retrieval stage {} took {} ms", stage, (System.nanoTime() - startNanos) / 1_000_000);
  }

  /** Runs a search path, swallowing a failure so fusion still proceeds with the other paths. */
  private static void degrade(String path, Runnable step) {
    long startNanos = System.nanoTime();
    try {
      step.run();
    } catch (RuntimeException e) {
      log.warn("Retrieval path {} failed, serving without it: {}", path, e.toString());
    } finally {
      log.debug("Retrieval path {} took {} ms", path, (System.nanoTime() - startNanos) / 1_000_000);
    }
  }
}
