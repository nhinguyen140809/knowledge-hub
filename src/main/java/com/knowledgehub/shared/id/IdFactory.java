package com.knowledgehub.shared.id;

/**
 * Derives <strong>stable, idempotent</strong> ids by hashing the identity (and content) of an
 * entity. Re-running ingestion/indexing on unchanged input produces the same id, so writes upsert
 * instead of creating duplicates.
 */
public final class IdFactory {

  /** NUL separator: cannot appear in paths/names/hashes, so distinct inputs never collide. */
  private static final String SEP = "\u0000";

  private IdFactory() {}

  /**
   * Builds a stable id from the given parts. The same parts always yield the same id.
   *
   * @param parts the identity components (order matters); must not be {@code null} or empty
   * @return a stable hex id
   */
  public static String stableId(String... parts) {
    if (parts == null || parts.length == 0) {
      throw new IllegalArgumentException("at least one part is required");
    }
    return Hashing.sha256(String.join(SEP, parts));
  }

  /** Stable chunk id from its source, file path, and content hash. */
  public static String chunkId(String sourceId, String path, String contentHash) {
    return stableId(sourceId, path, contentHash);
  }

  /** Stable code-entity id from its source, file path, and fully-qualified name. */
  public static String entityId(String sourceId, String path, String qualifiedName) {
    return stableId(sourceId, path, qualifiedName);
  }
}
