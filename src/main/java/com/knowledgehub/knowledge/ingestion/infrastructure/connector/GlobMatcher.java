package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/**
 * Decides whether a source-relative path is ingested, from its source's include/ignore globs. A
 * path is accepted when it matches at least one include glob (or include is empty) and matches no
 * ignore glob.
 *
 * <p>Beyond standard glob syntax ({@code **}/{@code *}/{@code ?}), a bare directory name (no glob
 * chars, no {@code /}) such as {@code node_modules} or {@code target} matches that name appearing
 * as <em>any</em> path segment — the common intent when ignoring a build/vendor directory.
 */
final class GlobMatcher {

  private final List<PathMatcher> includeGlobs;
  private final List<String> includeNames;
  private final List<PathMatcher> ignoreGlobs;
  private final List<String> ignoreNames;

  private GlobMatcher(List<String> include, List<String> ignore) {
    this.includeGlobs = compileGlobs(include);
    this.includeNames = plainNames(include);
    this.ignoreGlobs = compileGlobs(ignore);
    this.ignoreNames = plainNames(ignore);
  }

  static GlobMatcher forSource(Source source) {
    return new GlobMatcher(source.include(), source.ignore());
  }

  /** Whether the given source-relative path (forward-slash separated) is ingested. */
  boolean accepts(String relativePath) {
    Path path = Path.of(relativePath);
    boolean included =
        (includeGlobs.isEmpty() && includeNames.isEmpty())
            || includeGlobs.stream().anyMatch(m -> m.matches(path))
            || includeNames.stream().anyMatch(name -> hasSegment(path, name));
    if (!included) {
      return false;
    }
    return ignoreGlobs.stream().noneMatch(m -> m.matches(path))
        && ignoreNames.stream().noneMatch(name -> hasSegment(path, name));
  }

  private static boolean hasSegment(Path path, String name) {
    for (Path segment : path) {
      if (segment.toString().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private static List<PathMatcher> compileGlobs(List<String> patterns) {
    List<PathMatcher> matchers = new java.util.ArrayList<>();
    for (String pattern : patterns) {
      if (isPlainName(pattern)) {
        continue;
      }
      matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
      // Java's "**/" requires at least one directory, so "**/*.md" would miss a root-level file;
      // add the prefix-stripped variant so a leading "**/" also matches files at the root.
      if (pattern.startsWith("**/")) {
        matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern.substring(3)));
      }
    }
    return List.copyOf(matchers);
  }

  private static List<String> plainNames(List<String> patterns) {
    return patterns.stream().filter(GlobMatcher::isPlainName).toList();
  }

  private static boolean isPlainName(String pattern) {
    return pattern.chars().noneMatch(c -> "*?[]{}/".indexOf(c) >= 0);
  }
}
