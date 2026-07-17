package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import com.knowledgehub.knowledge.ingestion.domain.CommitRecord;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.domain.port.CommitHistoryPort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Component;

/**
 * {@link CommitHistoryPort} adapter for Git: walks the history from the HEAD of the source's
 * configured ref (newest first) and turns each commit into a {@link CommitRecord}. Changed paths
 * come from the diff against the first parent (against the empty tree for the root commit), so a
 * merge commit reports everything it brought onto the branch. The walk stops at {@code sinceSha} or
 * at the limit, whichever comes first.
 */
@Component
class GitCommitHistoryReader implements CommitHistoryPort {

  @Override
  public boolean supports(SourceType type) {
    return type == SourceType.GIT;
  }

  @Override
  public List<CommitRecord> history(Source source, String sinceSha, int limit) {
    if (limit <= 0) {
      return List.of();
    }
    try (GitWorkspace workspace = GitWorkspace.open(source)) {
      Repository repo = workspace.git().getRepository();
      ObjectId headId = repo.resolve(Constants.HEAD);
      if (headId == null) {
        return List.of();
      }
      try (RevWalk walk = new RevWalk(repo)) {
        walk.markStart(walk.parseCommit(headId));
        List<CommitRecord> records = new ArrayList<>();
        for (RevCommit commit : walk) {
          if (commit.getName().equals(sinceSha) || records.size() >= limit) {
            break;
          }
          records.add(toRecord(repo, walk, commit));
        }
        return records;
      }
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to read commit history of Git source " + source.sourceId(), e);
    }
  }

  private static CommitRecord toRecord(Repository repo, RevWalk walk, RevCommit commit)
      throws IOException {
    var author = commit.getAuthorIdent();
    return new CommitRecord(
        commit.getName(),
        commit.getFullMessage(),
        author.getName() + " <" + author.getEmailAddress() + ">",
        author.getWhenAsInstant(),
        changedPaths(repo, walk, commit));
  }

  /** The paths the commit's diff against its first parent touched (all paths for a root commit). */
  private static List<String> changedPaths(Repository repo, RevWalk walk, RevCommit commit)
      throws IOException {
    try (DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
      formatter.setRepository(repo);
      List<DiffEntry> entries;
      if (commit.getParentCount() > 0) {
        RevCommit parent = walk.parseCommit(commit.getParent(0));
        entries = formatter.scan(parent.getTree(), commit.getTree());
      } else {
        try (var reader = repo.newObjectReader()) {
          var current = new CanonicalTreeParser(null, reader, commit.getTree());
          entries = formatter.scan(new EmptyTreeIterator(), current);
        }
      }
      return entries.stream().map(GitCommitHistoryReader::pathOf).distinct().toList();
    }
  }

  /** A deleted file only has an old path; every other change carries its new path. */
  private static String pathOf(DiffEntry entry) {
    return entry.getChangeType() == DiffEntry.ChangeType.DELETE
        ? entry.getOldPath()
        : entry.getNewPath();
  }
}
