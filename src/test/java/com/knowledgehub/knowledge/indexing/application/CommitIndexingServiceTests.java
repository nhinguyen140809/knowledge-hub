package com.knowledgehub.knowledge.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.domain.CommitRepository;
import com.knowledgehub.knowledge.ingestion.domain.CommitHistoryPort;
import com.knowledgehub.knowledge.ingestion.domain.CommitRecord;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.config.AppProperties.Commits;
import com.knowledgehub.shared.id.IdFactory;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommitIndexingServiceTests {

  private final CommitHistoryPort historyPort = mock(CommitHistoryPort.class);
  private final CommitRepository commits = mock(CommitRepository.class);
  private final EmbeddingPort embedding = mock(EmbeddingPort.class);
  private final VectorStorePort vectorStore = mock(VectorStorePort.class);

  private final Source gitSource =
      new Source("g", SourceType.GIT, "/repo", "main", List.of(), List.of());

  private CommitIndexingService service(Integer depth) {
    return new CommitIndexingService(
        List.of(historyPort),
        commits,
        embedding,
        vectorStore,
        new AppProperties(null, null, new Commits(depth), null, null, null));
  }

  private static CommitRecord record(String sha, String message) {
    return new CommitRecord(sha, message, "t <t@x>", Instant.EPOCH, List.of("A.java"));
  }

  @Test
  void indexesOnlyTheCommitsNotStoredYet() {
    when(historyPort.supports(SourceType.GIT)).thenReturn(true);
    when(historyPort.history(eq(gitSource), any(), anyInt()))
        .thenReturn(List.of(record("c2", "newer"), record("c1", "older")));
    when(commits.existingShas(eq("g"), any())).thenReturn(Set.of("c1"));
    when(embedding.embedBatch(List.of("newer"))).thenReturn(List.of(new float[] {0.1f}));

    int indexed = service(100).index(gitSource, "c0");

    assertThat(indexed).isEqualTo(1);
    // The vector is written first, keyed like the graph node, so the node marks completion.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ChunkVector>> vectors = ArgumentCaptor.forClass(List.class);
    verify(vectorStore).upsert(vectors.capture());
    assertThat(vectors.getValue()).hasSize(1);
    assertThat(vectors.getValue().get(0).chunkId()).isEqualTo(IdFactory.commitId("g", "c2"));
    assertThat(vectors.getValue().get(0).metadata())
        .containsEntry("type", "commit")
        .containsEntry("source_id", "g")
        .containsEntry("commit_sha", "c2")
        .containsEntry("ref", "main");
    verify(commits)
        .upsertAll(eq("g"), eq("main"), any(Instant.class), eq(List.of(record("c2", "newer"))));
  }

  @Test
  void anUpToDateHistoryEmbedsNothing() {
    when(historyPort.supports(SourceType.GIT)).thenReturn(true);
    when(historyPort.history(eq(gitSource), any(), anyInt())).thenReturn(List.of());

    assertThat(service(100).index(gitSource, "head")).isZero();
    verifyNoInteractions(embedding, vectorStore);
    verify(commits, never()).upsertAll(anyString(), any(), any(), any());
  }

  @Test
  void aSourceTypeWithoutHistoryIndexesNothing() {
    Source fsSource = new Source("f", SourceType.FS, "/data", null, List.of(), List.of());
    when(historyPort.supports(SourceType.FS)).thenReturn(false);

    assertThat(service(100).index(fsSource, null)).isZero();
    verifyNoInteractions(commits, embedding, vectorStore);
  }

  @Test
  void aZeroDepthDisablesCommitIndexing() {
    assertThat(service(0).index(gitSource, null)).isZero();
    verifyNoInteractions(historyPort, commits, embedding, vectorStore);
  }

  @Test
  void aBlankMessageIsEmbeddedAsItsHash() {
    when(historyPort.supports(SourceType.GIT)).thenReturn(true);
    when(historyPort.history(eq(gitSource), any(), anyInt()))
        .thenReturn(List.of(record("c9", "  ")));
    when(commits.existingShas(eq("g"), any())).thenReturn(Set.of());
    when(embedding.embedBatch(List.of("c9"))).thenReturn(List.of(new float[] {0.2f}));

    assertThat(service(100).index(gitSource, null)).isEqualTo(1);
    verify(embedding).embedBatch(List.of("c9"));
  }
}
