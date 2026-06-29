package com.knowledgehub.knowledge.ingestion.domain;

import java.util.Objects;

/**
 * A single raw unit pulled from a source before chunking: its repo-/folder-relative {@code path},
 * the detected {@code mediaType}, the raw {@code content} bytes, the extracted {@code text} (filled
 * in by a {@link DocumentReader}; {@code null} until then), and its {@link Provenance}.
 *
 * <p>Separating the raw bytes from the extracted text lets ingestion fetch once and read by format
 * later — adding a format is one new {@link DocumentReader}, not a change here.
 */
public record RawArtifact(
    String path, String mediaType, byte[] content, String text, Provenance provenance) {

  public RawArtifact {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(provenance, "provenance");
    content = content.clone();
  }

  /** A raw artifact with no extracted text yet. */
  public static RawArtifact raw(
      String path, String mediaType, byte[] content, Provenance provenance) {
    return new RawArtifact(path, mediaType, content, null, provenance);
  }

  /** A copy of this artifact with the extracted text attached. */
  public RawArtifact withText(String extractedText) {
    return new RawArtifact(path, mediaType, content, extractedText, provenance);
  }

  /** Defensive copy — the content bytes are owned by this value object. */
  @Override
  public byte[] content() {
    return content.clone();
  }
}
