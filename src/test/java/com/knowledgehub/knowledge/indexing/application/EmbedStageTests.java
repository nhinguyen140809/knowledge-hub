package com.knowledgehub.knowledge.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbedStageTests {

  private final EmbeddingPort embedding = mock(EmbeddingPort.class);

  private static IndexingContext context() {
    return new IndexingContext(IndexingFixtures.markdownArtifact("x"), new ChunkConfig(512, 0));
  }

  @Test
  void embedsNewChunksAndAttachesMetadata() {
    Chunk chunk = IndexingFixtures.docChunk("body");
    IndexingContext context = context();
    context.setNewChunks(List.of(chunk), 0);
    when(embedding.embedBatch(List.of("body"))).thenReturn(List.of(new float[] {0.1f, 0.2f}));

    IndexingContext result = new EmbedStage(embedding).apply(context);

    assertThat(result.vectors()).hasSize(1);
    assertThat(result.vectors().get(0).chunkId()).isEqualTo(chunk.chunkId());
    assertThat(result.vectors().get(0).metadata())
        .containsEntry("source_id", IndexingFixtures.SOURCE)
        .containsEntry("type", "doc");
  }

  @Test
  void doesNothingWhenThereAreNoNewChunks() {
    IndexingContext result = new EmbedStage(embedding).apply(context());

    assertThat(result.vectors()).isEmpty();
    verifyNoInteractions(embedding);
  }
}
