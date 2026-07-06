package com.knowledgehub.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;

/**
 * Retrieval-eval logic shared by the mocked-embedding harness and the real-provider threshold
 * check: loading the labelled gold set, indexing the eval corpus, ranking a query through the
 * hybrid pipeline or the semantic path alone, and scoring Recall@10 and MRR.
 */
final class EvalHarness {

  static final String SOURCE = "eval-corpus";
  static final Path CORPUS = Path.of("src/test/resources/eval/corpus").toAbsolutePath();

  private EvalHarness() {}

  record EvalCase(String id, String query, String relevantPath) {}

  record EvalReport(String name, double recallAt10, double mrr) {}

  static List<EvalCase> loadGoldSet() {
    return loadGoldSet("eval/gold-set.json");
  }

  static List<EvalCase> loadGoldSet(String classpathResource) {
    try (InputStream in = new ClassPathResource(classpathResource).getInputStream()) {
      return new ObjectMapper().readValue(in, new TypeReference<List<EvalCase>>() {});
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static void indexCorpus(IndexingService indexing, SourceRepository sources) {
    indexCorpus(
        indexing,
        sources,
        new Source(SOURCE, SourceType.FS, CORPUS.toString(), null, List.of(), List.of()));
  }

  /**
   * Indexes an arbitrary source (e.g. a real Git repo) under its own id instead of {@link #SOURCE}.
   */
  static void indexCorpus(IndexingService indexing, SourceRepository sources, Source source) {
    sources.save(source);
    indexing.index(source.sourceId());
  }

  static void cleanUp(
      RetrievalCache cache,
      ChunkRepository chunks,
      CodeEntityRepository entities,
      VectorStorePort vectorStore,
      SourceRepository sources) {
    cleanUp(cache, chunks, entities, vectorStore, sources, SOURCE);
  }

  static void cleanUp(
      RetrievalCache cache,
      ChunkRepository chunks,
      CodeEntityRepository entities,
      VectorStorePort vectorStore,
      SourceRepository sources,
      String sourceId) {
    cache.invalidateAll();
    chunks.deleteBySource(sourceId);
    entities.deleteBySource(sourceId);
    vectorStore.deleteBySource(sourceId);
    sources.deleteById(sourceId);
  }

  static Function<String, List<String>> hybridRanking(RetrievalService retrievalService) {
    return query ->
        retrievalService.retrieve(Query.of(query), null).hits().stream()
            .map(hit -> hit.metadata().path())
            .toList();
  }

  static Function<String, List<String>> semanticRanking(
      VectorStorePort vectorStore, EmbeddingPort embeddingPort, RetrievalReadPort reader) {
    return query -> {
      List<ScoredId> scored =
          vectorStore.search(embeddingPort.embed(query), 10, Filter.unrestricted());
      Map<String, HitMetadata> metadata =
          reader.loadMetadata(
              scored.stream().map(ScoredId::chunkId).toList(), Filter.unrestricted());
      return scored.stream()
          .map(hit -> metadata.get(hit.chunkId()))
          .filter(Objects::nonNull)
          .map(HitMetadata::path)
          .toList();
    };
  }

  static EvalReport evaluate(
      String name, List<EvalCase> goldSet, Function<String, List<String>> ranking) {
    double recallSum = 0;
    double mrrSum = 0;
    for (EvalCase item : goldSet) {
      List<String> paths = ranking.apply(item.query());
      int rank = 0;
      for (int i = 0; i < Math.min(10, paths.size()); i++) {
        if (paths.get(i).endsWith(item.relevantPath())) {
          rank = i + 1;
          break;
        }
      }
      if (rank > 0) {
        recallSum += 1;
        mrrSum += 1.0 / rank;
      }
    }
    int n = goldSet.size();
    return new EvalReport(name, recallSum / n, mrrSum / n);
  }

  static void report(Logger log, int total, EvalReport... reports) {
    log.info("Retrieval eval over {} gold queries:", total);
    log.info(String.format("  %-9s | Recall@10 | MRR", "strategy"));
    for (EvalReport r : reports) {
      log.info(String.format("  %-9s |    %.3f  | %.3f", r.name(), r.recallAt10(), r.mrr()));
    }
  }
}
