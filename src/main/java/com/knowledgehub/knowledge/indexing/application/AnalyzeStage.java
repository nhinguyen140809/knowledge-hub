package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.analysis.domain.AnalysisResult;
import com.knowledgehub.knowledge.analysis.domain.LanguageAnalyzer;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * First filter: picks the first {@link LanguageAnalyzer} that supports the artifact (Strategy by
 * {@code supports}/order) and runs the single analysis pass — chunks, code entities, and the
 * relations/pending references the syntax decides. An artifact no analyzer handles, or that fails
 * to analyze, is marked skipped so the rest of the run continues.
 */
@Component
class AnalyzeStage implements Stage<IndexingContext> {

  private static final Logger log = LoggerFactory.getLogger(AnalyzeStage.class);

  private final List<LanguageAnalyzer> analyzers;

  AnalyzeStage(List<LanguageAnalyzer> analyzers) {
    this.analyzers = analyzers;
  }

  @Override
  public IndexingContext apply(IndexingContext context) {
    if (context.isSkipped()) {
      return context;
    }
    LanguageAnalyzer analyzer =
        analyzers.stream().filter(c -> c.supports(context.artifact())).findFirst().orElse(null);
    if (analyzer == null) {
      context.markSkipped("no analyzer for " + context.artifact().mediaType());
      log.debug(
          "No analyzer for artifact {} ({})",
          context.artifact().path(),
          context.artifact().mediaType());
      return context;
    }
    try {
      AnalysisResult result = analyzer.analyze(context.artifact(), context.config());
      context.setAnalyzed(
          result.chunks(), result.codeEntities(), result.relations(), result.pendingReferences());
    } catch (RuntimeException e) {
      context.markSkipped("analysis failed: " + e);
      log.warn("Skipping artifact {}: {}", context.artifact().path(), e.toString());
    }
    return context;
  }
}
