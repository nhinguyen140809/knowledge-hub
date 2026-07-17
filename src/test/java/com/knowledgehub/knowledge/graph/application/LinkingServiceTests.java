package com.knowledgehub.knowledge.graph.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.analysis.domain.PendingReference;
import com.knowledgehub.knowledge.domain.RelationType;
import com.knowledgehub.knowledge.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.port.CrossArtifactLinker;
import com.knowledgehub.knowledge.graph.domain.port.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.port.RelationshipRepository;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import com.knowledgehub.shared.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LinkingServiceTests {

  private final EntityResolver resolver = mock(EntityResolver.class);
  private final CrossArtifactLinker linker = mock(CrossArtifactLinker.class);
  private final RelationshipRepository repository = mock(RelationshipRepository.class);

  private static final RawArtifact DOC =
      RawArtifact.raw(
              "README.md",
              MediaTypes.MARKDOWN,
              "x".getBytes(StandardCharsets.UTF_8),
              new FsProvenance("src", "README.md", "hash", Instant.EPOCH))
          .withText("x");

  private LinkingService service() {
    return new LinkingService(
        resolver,
        List.of(linker),
        repository,
        new AppProperties(null, null, null, null, null, null));
  }

  @Test
  void writesLocalRelationsResolvedRefsAndAcceptedLinksAndDropsTheRest() {
    when(resolver.resolve(any(), any()))
        .thenReturn(Map.of("com.other.Base", "resolved")); // com.missing.X stays unresolved
    when(linker.supports(DOC)).thenReturn(true);
    when(linker.link(DOC, List.of()))
        .thenReturn(
            List.of(
                new LinkCandidate("doc", "kept", RelationType.DESCRIBES, 0.8, "Greeter"),
                new LinkCandidate("doc", "dropped", RelationType.DESCRIBES, 0.3, "Foo")));

    LinkSummary summary =
        service()
            .link(
                DOC,
                List.of(),
                List.of(Relationship.deterministic("a", "local", RelationType.CALLS)),
                List.of(
                    new PendingReference("a", "com.other.Base", RelationType.EXTENDS),
                    new PendingReference("a", "com.missing.X", RelationType.IMPORTS)));

    assertThat(summary.relationshipsWritten()).isEqualTo(3);
    assertThat(summary.candidatesDropped()).isEqualTo(1);

    ArgumentCaptor<List<Relationship>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).upsertAll(captor.capture());
    assertThat(captor.getValue())
        .extracting(Relationship::toId)
        .containsExactlyInAnyOrder("local", "resolved", "kept");
  }

  @Test
  void skipsResolutionWithoutPendingRefsAndLinkersThatDoNotSupportTheArtifact() {
    when(linker.supports(DOC)).thenReturn(false);

    LinkSummary summary = service().link(DOC, List.of(), List.of(), List.of());

    assertThat(summary.relationshipsWritten()).isZero();
    verifyNoInteractions(resolver);
    verify(repository).upsertAll(anyList());
  }
}
