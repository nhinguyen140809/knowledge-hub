package com.knowledgehub.shared.id;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministic SHA-256 content hashing. Used both for the {@code content_hash} that powers dedup
 * and as the basis for stable, idempotent ids (see {@link IdFactory}).
 */
public final class Hashing {

  private Hashing() {}

  /**
   * Returns the lowercase hex SHA-256 of the given content, encoded as UTF-8.
   *
   * @param content the text to hash; must not be {@code null}
   * @return 64-character lowercase hex digest
   */
  public static String sha256(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available in this JVM", e);
    }
  }
}
