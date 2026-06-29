package com.knowledgehub.knowledge.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RawArtifactTests {

  private static final Provenance PROV =
      new FsProvenance("src", "a.md", "hash", Instant.parse("2026-01-01T00:00:00Z"));

  @Test
  void rawHasNoExtractedTextYet() {
    RawArtifact artifact = RawArtifact.raw("a.md", "text/markdown", "hi".getBytes(), PROV);

    assertThat(artifact.text()).isNull();
    assertThat(artifact.mediaType()).isEqualTo("text/markdown");
  }

  @Test
  void withTextKeepsOtherFieldsAndSetsText() {
    RawArtifact artifact =
        RawArtifact.raw("a.md", "text/markdown", "hi".getBytes(), PROV).withText("clean text");

    assertThat(artifact.text()).isEqualTo("clean text");
    assertThat(artifact.path()).isEqualTo("a.md");
    assertThat(artifact.provenance()).isEqualTo(PROV);
  }

  @Test
  void contentBytesAreDefensivelyCopiedInAndOut() {
    byte[] original = {1, 2, 3};
    RawArtifact artifact = RawArtifact.raw("a.bin", "application/octet-stream", original, PROV);

    original[0] = 9; // mutate the caller's array
    artifact.content()[1] = 9; // mutate the returned array

    assertThat(artifact.content()).containsExactly(1, 2, 3);
  }
}
