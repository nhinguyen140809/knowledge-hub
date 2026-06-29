package com.knowledgehub.knowledge.ingestion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Neo4jSourceAdapterTests {

  @Autowired private SourceRepository repository;

  @Test
  void savesAndReadsBackAGitSource() {
    Source source =
        new Source(
            "it-git",
            SourceType.GIT,
            "https://example.com/repo.git",
            "main",
            List.of("**/*.java"),
            List.of("target"));
    try {
      repository.save(source);

      Source found = repository.findById("it-git").orElseThrow();
      assertThat(found.type()).isEqualTo(SourceType.GIT);
      assertThat(found.uriOrPath()).isEqualTo("https://example.com/repo.git");
      assertThat(found.ref()).contains("main");
      assertThat(found.include()).containsExactly("**/*.java");
      assertThat(found.ignore()).containsExactly("target");
    } finally {
      repository.deleteById("it-git");
    }
  }

  @Test
  void savesFilesystemSourceWithoutRefAndDeletes() {
    Source source = new Source("it-fs", SourceType.FS, "/data/docs", null, List.of(), List.of());
    repository.save(source);

    Source found = repository.findById("it-fs").orElseThrow();
    assertThat(found.type()).isEqualTo(SourceType.FS);
    assertThat(found.ref()).isEmpty();
    assertThat(repository.findAll()).extracting(Source::sourceId).contains("it-fs");

    repository.deleteById("it-fs");
    assertThat(repository.findById("it-fs")).isEmpty();
  }

  @Test
  void findByIdIsEmptyWhenMissing() {
    assertThat(repository.findById("does-not-exist")).isEmpty();
  }
}
