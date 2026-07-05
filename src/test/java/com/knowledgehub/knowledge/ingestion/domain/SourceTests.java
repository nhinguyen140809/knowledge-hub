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
  void withConfigReplacesEditablePartsAndKeepsIdentity() {
    Source original =
        new Source("src-1", SourceType.GIT, "https://x/y.git", "main", List.of("**/*.java"), null);

    Source updated = original.withConfig("dev", List.of("**/*.md"), List.of("target"));

    assertThat(updated.sourceId()).isEqualTo("src-1");
    assertThat(updated.type()).isEqualTo(SourceType.GIT);
    assertThat(updated.uriOrPath()).isEqualTo("https://x/y.git");
    assertThat(updated.ref()).contains("dev");
    assertThat(updated.include()).containsExactly("**/*.md");
    assertThat(updated.ignore()).containsExactly("target");
  }

  @Test
  void withConfigStillEnforcesInvariants() {
    Source fsSource = new Source("src-2", SourceType.FS, "/data", null, null, null);

    assertThatThrownBy(() -> fsSource.withConfig("main", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ref");
  }

  @Test
  void sourcesAreEqualByIdentity() {
    Source a = new Source("same", SourceType.GIT, "https://x/y.git", "main", null, null);
    Source b = new Source("same", SourceType.FS, "/other", null, null, null);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void carriesOptionalNameAndDescription() {
    Source source =
        new Source(
            "src", SourceType.FS, "/data", null, null, null, "  Design docs  ", "Team notes");

    assertThat(source.name()).contains("Design docs"); // trimmed
    assertThat(source.description()).contains("Team notes");
  }

  @Test
  void blankNameAndDescriptionAreAbsent() {
    Source source = new Source("src", SourceType.FS, "/data", null, null, null, " ", "");

    assertThat(source.name()).isEmpty();
    assertThat(source.description()).isEmpty();
  }

  @Test
  void withMetadataReplacesMetadataAndKeepsEverythingElse() {
    Source original =
        new Source(
            "src-1",
            SourceType.GIT,
            "https://x/y.git",
            "main",
            List.of("**/*.java"),
            List.of("target"),
            "Old",
            "Old desc");

    Source updated = original.withMetadata("New", "New desc");

    assertThat(updated.name()).contains("New");
    assertThat(updated.description()).contains("New desc");
    assertThat(updated.sourceId()).isEqualTo("src-1");
    assertThat(updated.ref()).contains("main");
    assertThat(updated.include()).containsExactly("**/*.java");
    assertThat(updated.ignore()).containsExactly("target");
  }

  @Test
  void withConfigPreservesMetadata() {
    Source original =
        new Source(
            "src-1", SourceType.GIT, "https://x/y.git", "main", null, null, "Repo", "A repo");

    Source updated = original.withConfig("dev", List.of("**/*.md"), null);

    assertThat(updated.ref()).contains("dev");
    assertThat(updated.name()).contains("Repo");
    assertThat(updated.description()).contains("A repo");
  }
}
