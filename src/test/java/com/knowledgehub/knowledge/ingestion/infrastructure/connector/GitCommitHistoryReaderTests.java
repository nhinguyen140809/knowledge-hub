package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.ingestion.domain.CommitRecord;
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

class GitCommitHistoryReaderTests {

  private final GitCommitHistoryReader reader = new GitCommitHistoryReader();

  private static void write(Path file, String body) throws Exception {
    Files.createDirectories(file.getParent());
    Files.writeString(file, body, StandardCharsets.UTF_8);
  }

  private static RevCommit commit(Git git, String message) throws Exception {
    git.add().addFilepattern(".").call();
    git.add().setUpdate(true).addFilepattern(".").call();
    return git.commit().setMessage(message).setAuthor("t", "t@x").setCommitter("t", "t@x").call();
  }

  @Test
  void walksHistoryNewestFirstWithChangedPaths(@TempDir Path repoDir) throws Exception {
    try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
      write(repoDir.resolve("A.java"), "class A {}");
      write(repoDir.resolve("B.java"), "class B {}");
      commit(git, "init");
      write(repoDir.resolve("A.java"), "class A { int x; }");
      commit(git, "grow A");
      Files.delete(repoDir.resolve("B.java"));
      commit(git, "drop B");
    }
    Source source = new Source("g", SourceType.GIT, repoDir.toString(), null, List.of(), List.of());

    List<CommitRecord> history = reader.history(source, null, 10);

    assertThat(history)
        .extracting(CommitRecord::message)
        .containsExactly("drop B", "grow A", "init");
    // A deleted file is reported by its old path; the root commit by its full tree.
    assertThat(history.get(0).changedPaths()).containsExactly("B.java");
    assertThat(history.get(1).changedPaths()).containsExactly("A.java");
    assertThat(history.get(2).changedPaths()).containsExactlyInAnyOrder("A.java", "B.java");
    assertThat(history.get(0).author()).isEqualTo("t <t@x>");
    assertThat(history.get(0).authoredAt()).isNotNull();
  }

  @Test
  void stopsAtTheLastKnownCommitAndAtTheLimit(@TempDir Path repoDir) throws Exception {
    String firstSha;
    try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
      write(repoDir.resolve("A.java"), "class A {}");
      firstSha = commit(git, "init").getName();
      write(repoDir.resolve("A.java"), "v2");
      commit(git, "second");
      write(repoDir.resolve("A.java"), "v3");
      commit(git, "third");
    }
    Source source = new Source("g", SourceType.GIT, repoDir.toString(), null, List.of(), List.of());

    assertThat(reader.history(source, firstSha, 10))
        .extracting(CommitRecord::message)
        .containsExactly("third", "second");
    assertThat(reader.history(source, null, 1))
        .extracting(CommitRecord::message)
        .containsExactly("third");
  }

  @Test
  void anUpToDateSourceYieldsNothing(@TempDir Path repoDir) throws Exception {
    String headSha;
    try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
      write(repoDir.resolve("A.java"), "class A {}");
      headSha = commit(git, "init").getName();
    }
    Source source = new Source("g", SourceType.GIT, repoDir.toString(), null, List.of(), List.of());

    assertThat(reader.history(source, headSha, 10)).isEmpty();
  }
}
