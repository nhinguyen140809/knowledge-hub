package com.knowledgehub.access.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministic SHA-256 hashing of a credential secret. Deterministic (no salt) on purpose: the
 * secret is a system-generated 256-bit token, so brute-force is infeasible without a slow KDF, and
 * a stable hash lets credentials be looked up by an indexed column in O(1) per request.
 */
public final class Sha256 {

  private Sha256() {}

  public static String hex(String input) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
