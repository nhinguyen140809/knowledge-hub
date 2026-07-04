package com.knowledgehub.shared.id;

/**
 * The id scheme: derives <strong>stable, idempotent</strong> ids by hashing an ordered set of
 * identity parts, so re-running ingestion/indexing on unchanged input produces the same id and
 * writes upsert instead of duplicating. Each entity owns which parts make up its id (e.g. {@code
 * Chunk.create}, {@code CodeEntity.deriveId}); this class only defines how parts become an id.
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

  /**
   * Stable file id from its source and file path. Kept here because a file has no domain class of
   * its own yet it is referenced by both {@code Chunk} and {@code CodeEntity}.
   */
  public static String fileId(String sourceId, String path) {
    return stableId(sourceId, path);
  }
}
