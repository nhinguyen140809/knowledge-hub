package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlobMatcherTests {

  private static GlobMatcher matcher(List<String> include, List<String> ignore) {
    return GlobMatcher.forSource(new Source("s", SourceType.FS, "/x", null, include, ignore));
  }

  @Test
  void emptyIncludeAcceptsEverythingNotIgnored() {
    GlobMatcher matcher = matcher(List.of(), List.of("target"));

    assertThat(matcher.accepts("src/Main.java")).isTrue();
    assertThat(matcher.accepts("target/Out.class")).isFalse();
  }

  @Test
  void includeGlobRestrictsToMatchingFiles() {
    GlobMatcher matcher = matcher(List.of("**/*.java"), List.of());

    assertThat(matcher.accepts("src/Main.java")).isTrue();
    assertThat(matcher.accepts("src/app.py")).isFalse();
  }

  @Test
  void bareNameIgnoresThatDirectoryAtAnyDepth() {
    GlobMatcher matcher = matcher(List.of(), List.of("node_modules"));

    assertThat(matcher.accepts("web/src/index.js")).isTrue();
    assertThat(matcher.accepts("web/node_modules/lib/x.js")).isFalse();
  }

  @Test
  void doubleStarPrefixAlsoMatchesRootLevelFiles() {
    GlobMatcher matcher = matcher(List.of("**/*.md"), List.of());

    assertThat(matcher.accepts("README.md")).isTrue();
    assertThat(matcher.accepts("docs/guide.md")).isTrue();
    assertThat(matcher.accepts("README.txt")).isFalse();
  }

  @Test
  void ignoreGlobByExtension() {
    GlobMatcher matcher = matcher(List.of(), List.of("**/*.class"));

    assertThat(matcher.accepts("build/A.class")).isFalse();
    assertThat(matcher.accepts("build/A.java")).isTrue();
  }
}
