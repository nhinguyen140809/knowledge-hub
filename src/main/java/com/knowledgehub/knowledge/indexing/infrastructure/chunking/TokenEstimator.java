package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

/**
 * Cheap, model-agnostic token estimate (≈ 4 characters per token, the rule of thumb for English and
 * code with BPE tokenizers). Good enough to size chunks; the exact count is the provider's concern
 * at embed time. A dedicated tokenizer can replace this without touching the chunkers.
 */
final class TokenEstimator {

  private static final int CHARS_PER_TOKEN = 4;

  private TokenEstimator() {}

  /** Estimated token count of the text (at least 1 for any non-empty text). */
  static int estimate(String text) {
    if (text.isEmpty()) {
      return 0;
    }
    return Math.max(1, (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN);
  }
}
