package com.knowledgehub.knowledge.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.analysis.infrastructure.DocAnalyzer;
import com.knowledgehub.knowledge.analysis.infrastructure.JavaAnalyzer;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.graph.application.LinkSummary;
import com.knowledgehub.knowledge.graph.application.LinkingService;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.infrastructure.lang.JavaLanguage;
import com.knowledgehub.knowledge.ingestion.application.IngestionService;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import com.knowledgehub.shared.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IndexingServiceTests {

  private final IngestionService ingestion = mock(IngestionService.class);
  private final EmbeddingPort embedding = mock(EmbeddingPort.class);
  private final VectorStorePort vectorStore = mock(VectorStorePort.class);
  private final ChunkRepository chunks = mock(ChunkRepository.class);
  private final CodeEntityRepository entities = mock(CodeEntityRepository.class);
  private final LinkingService linking = mock(LinkingService.class);

  {
    when(linking.link(any(), anyList(), anyList(), anyList())).thenReturn(LinkSummary.NONE);
  }

  private IndexingService service() {
    return new IndexingService(
        ingestion,
        new AppProperties(null, null, null, null, null, null),
        new AnalyzeStage(List.of(new JavaAnalyzer(new JavaLanguage()), new DocAnalyzer())),
        new DedupStage(chunks),
        new EmbedStage(embedding),
        new StoreStage(vectorStore, chunks, entities),
        new LinkStage(linking));
  }

  private static RawArtifact markdown() {
    return markdown("doc.md", "# Title\n\nbody paragraph.\n", "h1");
  }

  private static RawArtifact markdown(String path, String text, String hash) {
    return RawArtifact.raw(
            path,
            MediaTypes.MARKDOWN,
            text.getBytes(StandardCharsets.UTF_8),
            new FsProvenance("s", path, hash, Instant.EPOCH))
        .withText(text);
  }

  private static RawArtifact brokenJava() {
    String text = "this is not valid java {{{";
    return RawArtifact.raw(
            "Bad.java",
            MediaTypes.PLAIN_TEXT,
            text.getBytes(StandardCharsets.UTF_8),
            new FsProvenance("s", "Bad.java", "h2", Instant.EPOCH))
        .withText(text);
  }

  @Test
  void indexesGoodArtifactsAndSkipsBrokenOnesWithoutAborting() {
    when(ingestion.ingest("s")).thenReturn(Stream.of(markdown(), brokenJava()));
    when(chunks.existingContentHashes(eq("s"), any())).thenReturn(Set.of());
    when(embedding.embedBatch(anyList()))
        .thenAnswer(
            inv ->
                ((List<String>) inv.getArgument(0)).stream().map(t -> new float[] {1f}).toList());

    IndexResult result = service().index("s");

    assertThat(result.filesRead()).isEqualTo(1);
    assertThat(result.filesSkipped()).isEqualTo(1); // the broken Java file
    assertThat(result.chunksIndexed()).isGreaterThanOrEqualTo(1);
    assertThat(result.chunksCached()).isZero();
    verify(vectorStore, times(1)).upsert(anyList());
    verify(chunks, times(1)).upsertAll(anyList());
  }

  @Test
  void isolatesAMidPipelineFailureAndKeepsIndexingTheRest() {
    when(ingestion.ingest("s"))
        .thenReturn(Stream.of(markdown("first.md", "# First\n\none.\n", "h1"), markdown()));
    when(chunks.existingContentHashes(eq("s"), any())).thenReturn(Set.of());
    // The embedding provider fails for the first file, then recovers for the second.
    when(embedding.embedBatch(anyList()))
        .thenThrow(new RuntimeException("provider down"))
        .thenAnswer(
            inv ->
                ((List<String>) inv.getArgument(0)).stream().map(t -> new float[] {1f}).toList());

    IndexResult result = service().index("s");

    assertThat(result.filesSkipped()).isEqualTo(1); // the file whose embedding failed
    assertThat(result.filesRead()).isEqualTo(1); // the run continued past the failure
    verify(vectorStore, times(1)).upsert(anyList());
  }

  @Test
  void skipsEmbeddingAndStoreWhenContentIsAlreadyIndexed() {
    when(ingestion.ingest("s")).thenReturn(Stream.of(markdown()));
    when(chunks.existingContentHashes(eq("s"), any()))
        .thenAnswer(inv -> Set.copyOf(inv.getArgument(1)));

    IndexResult result = service().index("s");

    assertThat(result.chunksIndexed()).isZero();
    assertThat(result.chunksCached()).isGreaterThanOrEqualTo(1);
    verify(embedding, never()).embedBatch(anyList());
    verify(vectorStore, never()).upsert(anyList());
  }
}
