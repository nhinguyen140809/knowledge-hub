package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.shared.id.Hashing;
import com.knowledgehub.shared.id.IdFactory;

/**
 * Assembles a {@link Chunk} from a piece of an artifact, deriving the content hash, stable ids and
 * token estimate so the chunkers focus on <em>where</em> to cut, not on bookkeeping.
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
    String sourceId = artifact.provenance().sourceId();
    String path = artifact.path();
    String contentHash = Hashing.sha256(text);
    return new Chunk(
        IdFactory.chunkId(sourceId, path, contentHash),
        sourceId,
        IdFactory.fileId(sourceId, path),
        path,
        type,
        text,
        contentHash,
        TokenCounter.count(text),
        lineStart,
        lineEnd,
        entityId,
        artifact.provenance());
  }
}
