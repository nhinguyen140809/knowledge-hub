package com.knowledgehub.knowledge.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DedupStageTests {

  private final ChunkRepository repository = mock(ChunkRepository.class);

  @Test
  void dropsChunksWhoseContentHashIsAlreadyIndexed() {
    Chunk kept = IndexingFixtures.docChunk("new content");
    Chunk cached = IndexingFixtures.docChunk("old content");
    IndexingContext context =
        new IndexingContext(IndexingFixtures.markdownArtifact("x"), new ChunkConfig(512, 0));
    context.setAnalyzed(List.of(kept, cached), List.of(), List.of(), List.of());
    when(repository.existingContentHashes(eq(IndexingFixtures.SOURCE), any()))
        .thenReturn(Set.of(cached.contentHash()));

    IndexingContext result = new DedupStage(repository).apply(context);

    assertThat(result.newChunks()).containsExactly(kept);
    assertThat(result.cached()).isEqualTo(1);
  }
}
