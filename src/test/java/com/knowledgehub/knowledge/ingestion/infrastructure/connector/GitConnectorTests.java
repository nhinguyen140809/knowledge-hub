package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.ingestion.domain.GitProvenance;
import com.knowledgehub.knowledge.ingestion.domain.Provenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitConnectorTests {

  private final GitConnector connector = new GitConnector();

  private static void write(Path file, String body) throws Exception {
    Files.createDirectories(file.getParent());
    Files.writeString(file, body, StandardCharsets.UTF_8);
  }

  @Test
  void ingestsTrackedFilesWithGitProvenance(@TempDir Path repoDir) throws Exception {
    String sha;
    try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
      write(repoDir.resolve("A.java"), "class A {}");
      write(repoDir.resolve("docs/B.md"), "# B");
      git.add().addFilepattern(".").call();
      RevCommit commit =
          git.commit().setMessage("init").setAuthor("t", "t@x").setCommitter("t", "t@x").call();
      sha = commit.getName();
    }

    Source source = new Source("g", SourceType.GIT, repoDir.toString(), null, List.of(), List.of());

    List<RawArtifact> artifacts;
    try (var stream = connector.fetch(source)) {
      artifacts = stream.toList();
    }

    assertThat(artifacts)
        .extracting(RawArtifact::path)
        .containsExactlyInAnyOrder("A.java", "docs/B.md");

    Provenance provenance =
        artifacts.stream()
            .filter(a -> a.path().equals("A.java"))
            .findFirst()
            .orElseThrow()
            .provenance();
    assertThat(provenance).isInstanceOf(GitProvenance.class);
    GitProvenance git = (GitProvenance) provenance;
    assertThat(git.commitSha()).isEqualTo(sha);
    assertThat(git.ref()).isNotBlank();
    assertThat(git.contentHash()).matches("[0-9a-f]{64}");
  }

  @Test
  void supportsOnlyGitSources() {
    assertThat(connector.supports(SourceType.GIT)).isTrue();
    assertThat(connector.supports(SourceType.FS)).isFalse();
  }
}
