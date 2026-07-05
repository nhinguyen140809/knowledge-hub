package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An open Git repository for a source: a local repo is opened in place (checking out the configured
 * ref), a remote one is cloned into a temp directory that is removed on {@link #close}. Shared by
 * every adapter that reads a Git source, so open/clone/cleanup behaviour stays identical between
 * them.
 */
final class GitWorkspace implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(GitWorkspace.class);

  private final Git git;
  private final Path tempDir;

  private GitWorkspace(Git git, Path tempDir) {
    this.git = git;
    this.tempDir = tempDir;
  }

  /** Opens the source's repository, cloning it first when it is remote. */
  static GitWorkspace open(Source source) {
    Path local = Path.of(source.uriOrPath());
    if (Files.isDirectory(local) && Files.isDirectory(local.resolve(".git"))) {
      try {
        Git git = Git.open(local.toFile());
        if (source.ref().isPresent()) {
          git.checkout().setName(source.ref().get()).call();
        }
        return new GitWorkspace(git, null);
      } catch (IOException | GitAPIException e) {
        throw new IllegalStateException("Failed to open Git source " + source.sourceId(), e);
      }
    }
    Path temp = null;
    try {
      temp = Files.createTempDirectory("kh-git-");
      var clone = Git.cloneRepository().setURI(source.uriOrPath()).setDirectory(temp.toFile());
      source.ref().ifPresent(clone::setBranch);
      return new GitWorkspace(clone.call(), temp);
    } catch (IOException | GitAPIException e) {
      deleteRecursively(temp);
      throw new IllegalStateException("Failed to clone Git source " + source.sourceId(), e);
    }
  }

  Git git() {
    return git;
  }

  @Override
  public void close() {
    git.close();
    deleteRecursively(tempDir);
  }

  private static void deleteRecursively(Path dir) {
    if (dir == null || !Files.exists(dir)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(GitWorkspace::deleteQuietly);
    } catch (IOException e) {
      log.warn("Failed to clean up temporary clone {}: {}", dir, e.toString());
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.warn("Failed to delete {}: {}", path, e.toString());
    }
  }
}
