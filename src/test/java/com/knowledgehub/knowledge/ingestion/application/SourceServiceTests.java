package com.knowledgehub.knowledge.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.ingestion.application.port.SourceFreshness;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRegistered;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.domain.exception.DuplicateSourceException;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import com.knowledgehub.shared.config.SourceProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class SourceServiceTests {

  private final SourceRepository repository = org.mockito.Mockito.mock(SourceRepository.class);
  private final ApplicationEventPublisher events =
      org.mockito.Mockito.mock(ApplicationEventPublisher.class);
  private final SourceFreshness freshness = org.mockito.Mockito.mock(SourceFreshness.class);
  private final SourceService service =
      new SourceService(repository, events, freshness, new SourceProperties(null));

  private static SourceSpec spec(String id) {
    return new SourceSpec(
        id, SourceType.FS, "/data", null, List.of(), List.of(), "Docs " + id, "A folder of docs");
  }

  @Test
  void registerSavesAndPublishesEvent() {
    when(repository.findById("s1")).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Source saved = service.register(spec("s1"));

    assertThat(saved.sourceId()).isEqualTo("s1");
    assertThat(saved.name()).contains("Docs s1");
    assertThat(saved.description()).contains("A folder of docs");
    ArgumentCaptor<SourceRegistered> event = ArgumentCaptor.forClass(SourceRegistered.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().sourceId()).isEqualTo("s1");
  }

  @Test
  void registerRejectsDuplicate() {
    when(repository.findById("s1")).thenReturn(Optional.of(spec("s1").toSource()));

    assertThatThrownBy(() -> service.register(spec("s1")))
        .isInstanceOf(DuplicateSourceException.class);
    verify(repository, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void getThrowsWhenMissing() {
    when(repository.findById("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get("nope")).isInstanceOf(SourceNotFoundException.class);
  }

  @Test
  void updateReplacesConfigWhenPresent() {
    Source existing =
        new Source("s1", SourceType.GIT, "https://x/y.git", "main", List.of(), List.of());
    when(repository.findById("s1")).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Source updated = service.update("s1", "dev", List.of("**/*.md"), List.of("target"), null, null);

    assertThat(updated.ref()).contains("dev");
    assertThat(updated.include()).containsExactly("**/*.md");
    assertThat(updated.ignore()).containsExactly("target");
    ArgumentCaptor<Source> saved = ArgumentCaptor.forClass(Source.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().uriOrPath()).isEqualTo("https://x/y.git");
  }

  @Test
  void updateChangesNameAndDescription() {
    Source existing =
        new Source(
            "s1",
            SourceType.GIT,
            "https://x/y.git",
            "main",
            List.of(),
            List.of(),
            "Old name",
            "Old description");
    when(repository.findById("s1")).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Source updated = service.update("s1", null, null, null, "New name", "New description");

    assertThat(updated.name()).contains("New name");
    assertThat(updated.description()).contains("New description");
    // config is left untouched
    assertThat(updated.ref()).contains("main");
  }

  @Test
  void updateClearsMetadataWhenBlank() {
    Source existing =
        new Source(
            "s1", SourceType.FS, "/data", null, List.of(), List.of(), "Some name", "Some desc");
    when(repository.findById("s1")).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Source updated = service.update("s1", null, null, null, " ", "");

    assertThat(updated.name()).isEmpty();
    assertThat(updated.description()).isEmpty();
  }

  @Test
  void updateKeepsFieldsPassedAsNull() {
    Source existing =
        new Source(
            "s1",
            SourceType.GIT,
            "https://x/y.git",
            "main",
            List.of("**/*.java"),
            List.of("target"));
    when(repository.findById("s1")).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // change only ignore; ref and include passed as null must keep their current value
    Source updated = service.update("s1", null, null, List.of("build"), null, null);

    assertThat(updated.ref()).contains("main");
    assertThat(updated.include()).containsExactly("**/*.java");
    assertThat(updated.ignore()).containsExactly("build");
  }

  @Test
  void updateThrowsWhenMissing() {
    when(repository.findById("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update("nope", null, null, null, null, null))
        .isInstanceOf(SourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void removeThrowsWhenMissing() {
    when(repository.findById("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.remove("nope")).isInstanceOf(SourceNotFoundException.class);
    verify(repository, never()).deleteById(any());
  }

  @Test
  void removeDeletesWhenPresent() {
    when(repository.findById("s1")).thenReturn(Optional.of(spec("s1").toSource()));

    service.remove("s1");

    verify(repository).deleteById("s1");
  }

  @Test
  void summaryCountsSyncedNeverSyncedAndStale() {
    when(repository.findAll())
        .thenReturn(
            List.of(spec("fresh").toSource(), spec("stale").toSource(), spec("new").toSource()));
    Instant recent = Instant.now().minus(1, ChronoUnit.HOURS);
    Instant longAgo = Instant.now().minus(30, ChronoUnit.DAYS);
    when(freshness.lastIndexedAt(any()))
        .thenReturn(Map.of("fresh", recent, "stale", longAgo));
    SourceService withDefaultStaleness =
        new SourceService(repository, events, freshness, new SourceProperties(null));

    SourceSummary summary = withDefaultStaleness.summary();

    assertThat(summary.total()).isEqualTo(3);
    assertThat(summary.synced()).isEqualTo(2);
    assertThat(summary.neverSynced()).isEqualTo(1);
    assertThat(summary.stale()).isEqualTo(1);
    assertThat(summary.lastSyncAt()).isEqualTo(recent);
  }

  @Test
  void summaryReportsNoLastSyncWhenNothingHasEverSynced() {
    when(repository.findAll()).thenReturn(List.of(spec("new").toSource()));
    when(freshness.lastIndexedAt(any())).thenReturn(Map.of());

    SourceSummary summary = service.summary();

    assertThat(summary.total()).isEqualTo(1);
    assertThat(summary.synced()).isZero();
    assertThat(summary.neverSynced()).isEqualTo(1);
    assertThat(summary.stale()).isZero();
    assertThat(summary.lastSyncAt()).isNull();
  }
}
