package com.knowledgehub.knowledge.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SourceTests {

  @Test
  void gitSourceKeepsItsRef() {
    Source source =
        new Source("src-1", SourceType.GIT, "https://x/y.git", "main", List.of("**/*.java"), null);

    assertThat(source.type()).isEqualTo(SourceType.GIT);
    assertThat(source.ref()).contains("main");
    assertThat(source.include()).containsExactly("**/*.java");
    assertThat(source.ignore()).isEmpty();
  }

  @Test
  void filesystemSourceRejectsRef() {
    assertThatThrownBy(() -> new Source("src-2", SourceType.FS, "/data/docs", "main", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ref");
  }

  @Test
  void filesystemSourceWithoutRefIsValid() {
    Source source = new Source("src-2", SourceType.FS, "/data/docs", null, null, null);

    assertThat(source.type()).isEqualTo(SourceType.FS);
    assertThat(source.ref()).isEmpty();
  }

  @Test
  void blankIdentityFieldsAreRejected() {
    assertThatThrownBy(() -> new Source(" ", SourceType.FS, "/data", null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sourceId");
    assertThatThrownBy(() -> new Source("src", SourceType.FS, " ", null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("uriOrPath");
  }

  @Test
  void includeAndIgnoreAreDefensivelyCopied() {
    var include = new java.util.ArrayList<>(List.of("a"));
    Source source = new Source("src", SourceType.FS, "/data", null, include, null);
    include.add("b");

    assertThat(source.include()).containsExactly("a");
  }

  @Test
  void sourcesAreEqualByIdentity() {
    Source a = new Source("same", SourceType.GIT, "https://x/y.git", "main", null, null);
    Source b = new Source("same", SourceType.FS, "/other", null, null, null);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }
}
