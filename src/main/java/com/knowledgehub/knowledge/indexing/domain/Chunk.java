package com.knowledgehub.knowledge.indexing.domain;

import com.knowledgehub.knowledge.ingestion.domain.Provenance;
import com.knowledgehub.shared.id.Hashing;
import com.knowledgehub.shared.id.IdFactory;
import java.util.Objects;

/**
 * A unit of indexable content cut from one artifact: a function/class for code, a section/paragraph
 * for documents. The {@code chunkId} is derived from {@code (sourceId, path, contentHash)} so
 * re-indexing identical content yields the same id and upserts in place (idempotent). Its vector
 * lives in the vector store and its node in the graph, joined by {@code chunkId}.
 *
 * @param chunkId stable, content-derived id
 * @param sourceId the source this chunk came from
 * @param fileId stable id of the file the chunk belongs to ({@code (sourceId, path)})
 * @param path the file's path within its source
 * @param type whether this is code or document content
 * @param text the chunk text (never blank)
 * @param contentHash SHA-256 of {@code text}, the dedup key
 * @param tokenCount estimated token count of {@code text}
 * @param lineStart first source line of the chunk (1-based, inclusive)
 * @param lineEnd last source line of the chunk (1-based, inclusive)
 * @param entityId the code entity this chunk belongs to, or {@code null} for non-code chunks
 * @param provenance origin coordinates traced back to the source
 */
public record Chunk(
    String chunkId,
    String sourceId,
    String fileId,
    String path,
    ChunkType type,
    String text,
    String contentHash,
    int tokenCount,
    int lineStart,
    int lineEnd,
    String entityId,
    Provenance provenance) {

  public Chunk {
    Objects.requireNonNull(chunkId, "chunkId");
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(fileId, "fileId");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(contentHash, "contentHash");
    Objects.requireNonNull(provenance, "provenance");
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("chunk text must not be blank");
    }
    if (tokenCount < 0) {
      throw new IllegalArgumentException("tokenCount must not be negative");
    }
    if (lineStart < 1 || lineEnd < lineStart) {
      throw new IllegalArgumentException("invalid line range: " + lineStart + ".." + lineEnd);
    }
  }

  /**
   * Mints a new chunk from a piece of content, deriving its content hash and stable ids so identity
   * stays consistent with the text (re-indexing the same content yields the same {@code chunkId}).
   * Use this for freshly cut chunks; the canonical constructor is for reconstituting a chunk whose
   * ids are already known (e.g. read back from storage).
   *
   * @param tokenCount estimated token count of {@code text} (counting is the caller's concern)
   */
  public static Chunk create(
      Provenance provenance,
      String path,
      ChunkType type,
      String text,
      int tokenCount,
      int lineStart,
      int lineEnd,
      String entityId) {
    String sourceId = provenance.sourceId();
    String contentHash = Hashing.sha256(text);
    return new Chunk(
        deriveId(sourceId, path, contentHash),
        sourceId,
        IdFactory.fileId(sourceId, path),
        path,
        type,
        text,
        contentHash,
        tokenCount,
        lineStart,
        lineEnd,
        entityId,
        provenance);
  }

  /**
   * Derives the stable id of the chunk with these coordinates and content hash. The same content at
   * the same path always yields the same id, so re-indexing unchanged content upserts in place.
   */
  public static String deriveId(String sourceId, String path, String contentHash) {
    return IdFactory.stableId(sourceId, path, contentHash);
  }
}
