package com.knowledgehub.knowledge.analysis.domain;

import com.knowledgehub.knowledge.domain.Relationship;
import java.util.List;

/**
 * What a {@link LanguageAnalyzer} produces for one artifact in a single pass: the chunks, any code
 * entities extracted alongside them, and what the syntax decides about relationships — edges whose
 * both ends live in the artifact (already resolved) plus pending references whose targets are
 * resolved by the linking step once the artifact's nodes are stored. Document analyzers return
 * chunks only.
 *
 * @param chunks the chunks cut from the artifact (never {@code null})
 * @param codeEntities entities extracted from the artifact (empty for non-code)
 * @param relations same-artifact edges, fully resolved at analysis time (empty for non-code)
 * @param pendingReferences named references awaiting resolution by linking (empty for non-code)
 */
public record AnalysisResult(
    List<Chunk> chunks,
    List<CodeEntity> codeEntities,
    List<Relationship> relations,
    List<PendingReference> pendingReferences) {

  public AnalysisResult {
    chunks = List.copyOf(chunks);
    codeEntities = List.copyOf(codeEntities);
    relations = List.copyOf(relations);
    pendingReferences = List.copyOf(pendingReferences);
  }

  /** Result with chunks only (no code entities, no relations). */
  public static AnalysisResult ofChunks(List<Chunk> chunks) {
    return new AnalysisResult(chunks, List.of(), List.of(), List.of());
  }
}
