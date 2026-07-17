package com.knowledgehub.knowledge.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.graph.application.LinkSummary;
import com.knowledgehub.knowledge.graph.application.LinkingService;
import org.junit.jupiter.api.Test;

class LinkStageTests {

  private final LinkingService linking = mock(LinkingService.class);

  @Test
  void linksTheArtifactAndRecordsTheCount() {
    when(linking.link(any(), anyList())).thenReturn(new LinkSummary(4, 1));
    IndexingContext context =
        new IndexingContext(IndexingFixtures.markdownArtifact("x"), new ChunkConfig(512, 0));
    context.setAnalyzed(
        java.util.List.of(IndexingFixtures.docChunk("body")),
        java.util.List.of(),
        java.util.List.of(),
        java.util.List.of());

    IndexingContext result = new LinkStage(linking).apply(context);

    assertThat(result.relationshipsLinked()).isEqualTo(4);
    verify(linking).link(any(), anyList());
  }

  @Test
  void skipsLinkingForASkippedArtifact() {
    IndexingContext context =
        new IndexingContext(IndexingFixtures.markdownArtifact("x"), new ChunkConfig(512, 0));
    context.markSkipped("no analyzer");

    new LinkStage(linking).apply(context);

    verify(linking, never()).link(any(), anyList());
  }
}
