package com.knowledgehub.knowledge.analysis.domain;

import java.util.List;

/**
 * What a {@link Chunker} produces for one artifact: the chunks plus any code entities extracted
 * alongside them. Document chunkers return no entities.
 *
 * @param chunks the chunks cut from the artifact (never {@code null})
 * @param codeEntities entities extracted from the artifact (empty for non-code)
 */
public record ChunkingResult(List<Chunk> chunks, List<CodeEntity> codeEntities) {

  public ChunkingResult {
    chunks = List.copyOf(chunks);
    codeEntities = List.copyOf(codeEntities);
  }

  /** Result with chunks only (no code entities). */
  public static ChunkingResult ofChunks(List<Chunk> chunks) {
    return new ChunkingResult(chunks, List.of());
  }
}
