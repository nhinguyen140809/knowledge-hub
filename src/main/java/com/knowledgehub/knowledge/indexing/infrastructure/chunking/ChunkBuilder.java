package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;

/**
 * Bridges an artifact span to the domain {@link Chunk} factory: counts the tokens — the one piece
 * that needs the tokenizer — and lets the domain derive the content hash and stable ids. This keeps
 * the chunkers focused on <em>where</em> to cut, not on bookkeeping.
 */
final class ChunkBuilder {

  private ChunkBuilder() {}

  /**
   * Builds a chunk for the given text span of an artifact.
   *
   * @param entityId the code entity this chunk belongs to, or {@code null} for documents
   */
  static Chunk build(
      RawArtifact artifact,
      ChunkType type,
      String text,
      int lineStart,
      int lineEnd,
      String entityId) {
    return Chunk.create(
        artifact.provenance(),
        artifact.path(),
        type,
        text,
        TokenCounter.count(text),
        lineStart,
        lineEnd,
        entityId);
  }
}
