package com.knowledgehub.knowledge.graph.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.graph.domain.CrossArtifactLinker;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.RelationshipRepository;
import com.knowledgehub.knowledge.graph.domain.StructuralExtractor;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import com.knowledgehub.shared.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LinkingServiceTests {

  private final StructuralExtractor extractor = mock(StructuralExtractor.class);
  private final CrossArtifactLinker linker = mock(CrossArtifactLinker.class);
  private final RelationshipRepository repository = mock(RelationshipRepository.class);

  private static final RawArtifact DOC =
      RawArtifact.raw(
              "README.md",
              MediaTypes.MARKDOWN,
              "x".getBytes(StandardCharsets.UTF_8),
              new FsProvenance("src", "README.md", "hash", Instant.EPOCH))
          .withText("x");

  @Test
  void writesStructuralAndAcceptedLinksAndDropsLowConfidenceOnes() {
    when(extractor.supports(DOC)).thenReturn(true);
    when(extractor.extract(DOC))
        .thenReturn(List.of(Relationship.deterministic("a", "b", RelationType.CALLS)));
    when(linker.supports(DOC)).thenReturn(true);
    when(linker.link(DOC, List.of()))
        .thenReturn(
            List.of(
                new LinkCandidate("doc", "kept", RelationType.DESCRIBES, 0.8, "Greeter"),
                new LinkCandidate("doc", "dropped", RelationType.DESCRIBES, 0.3, "Foo")));

    LinkingService service =
        new LinkingService(
            List.of(extractor),
            List.of(linker),
            repository,
            new AppProperties(null, null, null, null, null, null));

    LinkSummary summary = service.link(DOC, List.of());

    assertThat(summary.relationshipsWritten()).isEqualTo(2);
    assertThat(summary.candidatesDropped()).isEqualTo(1);

    ArgumentCaptor<List<Relationship>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).upsertAll(captor.capture());
    assertThat(captor.getValue())
        .extracting(Relationship::toId)
        .containsExactlyInAnyOrder("b", "kept");
  }

  @Test
  void skipsLinkersThatDoNotSupportTheArtifact() {
    when(extractor.supports(DOC)).thenReturn(false);
    when(linker.supports(DOC)).thenReturn(false);

    LinkingService service =
        new LinkingService(
            List.of(extractor),
            List.of(linker),
            repository,
            new AppProperties(null, null, null, null, null, null));

    LinkSummary summary = service.link(DOC, List.of());

    assertThat(summary.relationshipsWritten()).isZero();
    verify(repository).upsertAll(anyList());
  }
}
