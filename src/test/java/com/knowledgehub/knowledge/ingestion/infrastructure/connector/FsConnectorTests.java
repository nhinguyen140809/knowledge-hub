package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.Provenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FsConnectorTests {

  private final FsConnector connector = new FsConnector();

  private static void write(Path file, String body) throws Exception {
    Files.createDirectories(file.getParent());
    Files.writeString(file, body, StandardCharsets.UTF_8);
  }

  @Test
  void ingestsIncludedFilesWithProvenanceAndSkipsIgnored(@TempDir Path root) throws Exception {
    write(root.resolve("src/Main.java"), "class Main {}");
    write(root.resolve("README.md"), "# Title");
    write(root.resolve("target/Out.class"), "binary");

    Source source =
        new Source(
            "src-fs",
            SourceType.FS,
            root.toString(),
            null,
            List.of("**/*.java", "**/*.md"),
            List.of("target"));

    List<RawArtifact> artifacts;
    try (var stream = connector.fetch(source)) {
      artifacts = stream.toList();
    }

    assertThat(artifacts)
        .extracting(RawArtifact::path)
        .containsExactlyInAnyOrder("src/Main.java", "README.md");

    RawArtifact java =
        artifacts.stream().filter(a -> a.path().equals("src/Main.java")).findFirst().orElseThrow();
    assertThat(java.mediaType()).isEqualTo(MediaTypes.PLAIN_TEXT);
    Provenance provenance = java.provenance();
    assertThat(provenance).isInstanceOf(FsProvenance.class);
    assertThat(provenance.sourceId()).isEqualTo("src-fs");
    assertThat(provenance.path()).isEqualTo("src/Main.java");
    assertThat(provenance.contentHash()).matches("[0-9a-f]{64}");
  }

  @Test
  void supportsOnlyFilesystemSources() {
    assertThat(connector.supports(SourceType.FS)).isTrue();
    assertThat(connector.supports(SourceType.GIT)).isFalse();
  }

  @Test
  void rejectsNonDirectoryPath(@TempDir Path root) throws Exception {
    Path file = root.resolve("a.txt");
    Files.writeString(file, "x");
    Source source = new Source("bad", SourceType.FS, file.toString(), null, null, null);

    assertThatThrownBy(() -> connector.fetch(source))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a directory");
  }
}
