package com.knowledgehub.knowledge.indexing.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkConfig;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoreStageTests {

  private final VectorStorePort vectorStore = mock(VectorStorePort.class);
  private final ChunkRepository chunks = mock(ChunkRepository.class);
  private final CodeEntityRepository entities = mock(CodeEntityRepository.class);

  private StoreStage stage() {
    return new StoreStage(vectorStore, chunks, entities);
  }

  private static IndexingContext context() {
    return new IndexingContext(IndexingFixtures.markdownArtifact("x"), new ChunkConfig(512, 0));
  }

  @Test
  void persistsVectorsEntitiesAndChunks() {
    Chunk chunk = IndexingFixtures.docChunk("body");
    IndexingContext context = context();
    context.setNewChunks(List.of(chunk), 0);
    context.setVectors(List.of(new ChunkVector(chunk.chunkId(), new float[] {1f}, Map.of())));

    stage().apply(context);

    verify(entities).upsertAll(context.entities());
    verify(vectorStore).upsert(context.vectors());
    verify(chunks).upsertAll(context.newChunks());
  }

  @Test
  void writesNothingWhenThereAreNoNewChunks() {
    stage().apply(context());

    verifyNoInteractions(vectorStore, chunks, entities);
  }
}
