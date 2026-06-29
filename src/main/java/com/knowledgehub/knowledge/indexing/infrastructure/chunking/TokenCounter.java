package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

/**
 * Exact token counting with the {@code cl100k_base} BPE encoding (what {@code text-embedding-3-*}
 * uses), so chunks are sized to the model's real token budget rather than a character heuristic.
 * The encoding is thread-safe and built once.
 */
final class TokenCounter {

  private static final Encoding ENCODING =
      Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

  private TokenCounter() {}

  /** The number of tokens in the text. */
  static int count(String text) {
    return ENCODING.countTokens(text);
  }
}
