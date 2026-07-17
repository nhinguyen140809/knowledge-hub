package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import com.knowledgehub.knowledge.ingestion.domain.GitProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.domain.port.Connector;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import com.knowledgehub.shared.id.Hashing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Connector for Git sources: opens the repository via {@link GitWorkspace} (local in place, remote
 * cloned to a temp directory), resolves the current {@code commitSha} at the configured {@code ref}
 * (default branch otherwise), and emits one {@link RawArtifact} per tracked file at that commit
 * with {@link GitProvenance}. The workspace is released when the returned stream is closed.
 */
@Component
class GitConnector implements Connector {

  private static final Logger log = LoggerFactory.getLogger(GitConnector.class);
  private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

  @Override
  public boolean supports(SourceType type) {
    return type == SourceType.GIT;
  }

  @Override
  public Stream<RawArtifact> fetch(Source source) {
    GitWorkspace workspace = GitWorkspace.open(source);
    try {
      Repository repo = workspace.git().getRepository();
      ObjectId headId = repo.resolve(Constants.HEAD);
      if (headId == null) {
        throw new IllegalStateException("Git source has no HEAD commit: " + source.sourceId());
      }
      String commitSha = headId.getName();
      String ref = source.ref().orElseGet(() -> currentBranch(repo));
      GlobMatcher matcher = GlobMatcher.forSource(source);
      Instant indexedAt = Instant.now();
      List<Entry> entries = listEntries(repo, headId, matcher);
      return entries.stream()
          .map(entry -> readArtifact(source, repo, entry, ref, commitSha, indexedAt))
          .flatMap(Optional::stream)
          .onClose(workspace::close);
    } catch (IOException e) {
      workspace.close();
      throw new UncheckedIOException("Failed to read Git source " + source.sourceId(), e);
    } catch (RuntimeException e) {
      workspace.close();
      throw e;
    }
  }

  private List<Entry> listEntries(Repository repo, ObjectId headId, GlobMatcher matcher)
      throws IOException {
    List<Entry> entries = new ArrayList<>();
    try (RevWalk revWalk = new RevWalk(repo)) {
      RevCommit commit = revWalk.parseCommit(headId);
      try (TreeWalk treeWalk = new TreeWalk(repo)) {
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
          String path = treeWalk.getPathString();
          if (matcher.accepts(path)) {
            entries.add(new Entry(path, treeWalk.getObjectId(0)));
          }
        }
      }
    }
    return entries;
  }

  private Optional<RawArtifact> readArtifact(
      Source source,
      Repository repo,
      Entry entry,
      String ref,
      String commitSha,
      Instant indexedAt) {
    try {
      ObjectLoader loader = repo.open(entry.blobId, Constants.OBJ_BLOB);
      if (loader.getSize() > MAX_FILE_BYTES) {
        log.warn(
            "Skipping oversized file {} ({} bytes) in source {}",
            entry.path,
            loader.getSize(),
            source.sourceId());
        return Optional.empty();
      }
      byte[] content = loader.getBytes();
      GitProvenance provenance =
          new GitProvenance(
              source.sourceId(), entry.path, Hashing.sha256(content), indexedAt, ref, commitSha);
      return Optional.of(
          RawArtifact.raw(entry.path, MediaTypes.fromPath(entry.path), content, provenance));
    } catch (IOException | RuntimeException e) {
      log.warn(
          "Skipping unreadable file {} in source {}: {}",
          entry.path,
          source.sourceId(),
          e.toString());
      return Optional.empty();
    }
  }

  private static String currentBranch(Repository repo) {
    try {
      return repo.getBranch();
    } catch (IOException e) {
      return Constants.HEAD;
    }
  }

  /** A tracked file at the resolved commit: its path and blob object id. */
  private record Entry(String path, ObjectId blobId) {}
}
