package com.knowledgehub.knowledge.sync.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.ingestion.application.SourceDeleted;
import com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.sync.domain.ChangeSet;
import com.knowledgehub.knowledge.sync.domain.Evictor;
import com.knowledgehub.knowledge.sync.domain.FreshnessInfo;
import com.knowledgehub.knowledge.sync.domain.FreshnessRepository;
import com.knowledgehub.knowledge.sync.domain.SourceDiffer;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SyncServiceTests {

  private final SourceRepository sources = mock(SourceRepository.class);
  private final SourceDiffer differ = mock(SourceDiffer.class);
  private final IndexingService indexing = mock(IndexingService.class);
  private final Evictor evictor = mock(Evictor.class);
  private final FreshnessRepository freshness = mock(FreshnessRepository.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

  private final Source source =
      new Source("s1", SourceType.FS, "/repo", null, List.of("**/*.java"), List.of());

  private SyncService service;

  @BeforeEach
  void setUp() {
    service = new SyncService(sources, List.of(differ), indexing, evictor, freshness, events);
    when(sources.findById("s1")).thenReturn(Optional.of(source));
    when(differ.supports(any())).thenReturn(true);
  }

  @Test
  void aNoChangeSyncIsAnIdempotentNoOp() {
    when(differ.diff(any()))
        .thenReturn(new ChangeSet("s1", List.of(), List.of(), List.of(), 3, null));

    SyncResult result = service.sync("s1");

    assertThat(result.idempotent()).isTrue();
    assertThat(result.skipped()).isEqualTo(3);
    verify(indexing, never()).reindex(any(), any());
    verify(evictor, never()).evictFiles(any(), any());
    verify(freshness, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void routesAddedModifiedAndDeletedFiles() {
    when(differ.diff(any()))
        .thenReturn(
            new ChangeSet(
                "s1", List.of("New.java"), List.of("Mod.java"), List.of("Del.java"), 5, "abc123"));
    when(indexing.reindex(eq("s1"), any()))
        .thenReturn(Map.of("New.java", List.of("c3"), "Mod.java", List.of("c1", "c2")));

    SyncResult result = service.sync("s1");

    assertThat(result.indexed()).isEqualTo(1);
    assertThat(result.reindexed()).isEqualTo(1);
    assertThat(result.evicted()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(5);
    assertThat(result.idempotent()).isFalse();
    assertThat(result.toCommit()).isEqualTo("abc123");

    verify(evictor).evictFiles("s1", List.of("Del.java"));
    verify(indexing).reindex("s1", new HashSet<>(List.of("New.java", "Mod.java")));
    // A modified file keeps the chunks it still has; the rest are evicted.
    verify(evictor).retainChunks("s1", "Mod.java", List.of("c1", "c2"));
    verify(freshness).save(any(FreshnessInfo.class));
    verify(events).publishEvent(any(IndexCompleted.class));
  }

  @Test
  void aModifiedFileWhoseReindexWasSkippedKeepsItsExistingChunks() {
    when(differ.diff(any()))
        .thenReturn(new ChangeSet("s1", List.of(), List.of("Mod.java"), List.of(), 0, null));
    // Re-index produced no entry for the path (it was skipped or failed), not an empty chunk list.
    when(indexing.reindex(eq("s1"), any())).thenReturn(Map.of());

    service.sync("s1");

    verify(evictor, never()).retainChunks(any(), any(), any());
  }

  @Test
  void syncingAnUnknownSourceThrows() {
    when(sources.findById("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.sync("nope")).isInstanceOf(SourceNotFoundException.class);
  }

  @Test
  void removingASourceEvictsAllItsKnowledge() {
    service.onSourceDeleted(new SourceDeleted("s1"));

    verify(evictor).evictSource("s1");
    verify(freshness).deleteBySource("s1");
    verify(events).publishEvent(any(IndexCompleted.class));
  }
}
