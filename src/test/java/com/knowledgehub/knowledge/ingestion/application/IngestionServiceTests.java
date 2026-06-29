package com.knowledgehub.knowledge.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.ingestion.domain.Connector;
import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IngestionServiceTests {

  private final SourceRepository sources = Mockito.mock(SourceRepository.class);

  private static RawArtifact artifact(String path, String body) {
    return RawArtifact.raw(
        path,
        "text/plain",
        body.getBytes(StandardCharsets.UTF_8),
        new FsProvenance("s", path, "hash", Instant.parse("2026-01-01T00:00:00Z")));
  }

  /** Connector that yields the given artifacts for an FS source. */
  private static Connector connectorYielding(RawArtifact... artifacts) {
    return new Connector() {
      @Override
      public boolean supports(SourceType type) {
        return type == SourceType.FS;
      }

      @Override
      public Stream<RawArtifact> fetch(Source source) {
        return Stream.of(artifacts);
      }
    };
  }

  /** Reader that returns the UTF-8 text, but throws for any artifact whose body is "BAD". */
  private static DocumentReader flakyReader() {
    return new DocumentReader() {
      @Override
      public boolean supports(String mediaType) {
        return true;
      }

      @Override
      public String extractText(RawArtifact artifact) {
        String text = new String(artifact.content(), StandardCharsets.UTF_8);
        if (text.equals("BAD")) {
          throw new IllegalStateException("cannot parse " + artifact.path());
        }
        return text;
      }
    };
  }

  private IngestionService service(Connector connector, DocumentReader reader) {
    when(sources.findById("s"))
        .thenReturn(
            Optional.of(new Source("s", SourceType.FS, "/data", null, List.of(), List.of())));
    return new IngestionService(sources, List.of(connector), List.of(reader));
  }

  @Test
  void attachesExtractedTextToEachArtifact() {
    IngestionService service =
        service(
            connectorYielding(artifact("a.txt", "Alpha"), artifact("b.txt", "Beta")),
            flakyReader());

    List<RawArtifact> result;
    try (var stream = service.ingest("s")) {
      result = stream.toList();
    }

    assertThat(result).extracting(RawArtifact::text).containsExactly("Alpha", "Beta");
  }

  @Test
  void skipsArtifactWhoseReaderFailsButKeepsTheRest() {
    IngestionService service =
        service(
            connectorYielding(artifact("ok.txt", "Good"), artifact("bad.txt", "BAD")),
            flakyReader());

    List<RawArtifact> result;
    try (var stream = service.ingest("s")) {
      result = stream.toList();
    }

    assertThat(result).extracting(RawArtifact::path).containsExactly("ok.txt");
  }

  @Test
  void throwsWhenSourceMissing() {
    when(sources.findById("missing")).thenReturn(Optional.empty());
    IngestionService service = new IngestionService(sources, List.of(), List.of());

    assertThatThrownBy(() -> service.ingest("missing")).isInstanceOf(SourceNotFoundException.class);
  }
}
