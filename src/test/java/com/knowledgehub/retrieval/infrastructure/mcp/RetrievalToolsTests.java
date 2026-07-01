package com.knowledgehub.retrieval.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.access.infrastructure.security.AclFilterProvider;
import com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RankedResult;
import com.knowledgehub.shared.error.ErrorCode;
import com.knowledgehub.shared.error.ToolFailure;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RetrievalToolsTests {

  private final RetrievalService retrievalService = mock(RetrievalService.class);
  private final AclFilterProvider aclFilterProvider = mock(AclFilterProvider.class);
  private final RetrievalTools tools = new RetrievalTools(retrievalService, aclFilterProvider);

  @Test
  void queriesTheServiceScopedToTheCallersReadableSources() {
    when(aclFilterProvider.currentAllowedSources()).thenReturn(Set.of("s-a", "s-b"));
    when(retrievalService.retrieve(any(), any())).thenReturn(RankedResult.empty());

    tools.queryKnowledge("cache keying", 5, "main", "doc");

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<String>> allowed = ArgumentCaptor.forClass(Set.class);
    verify(retrievalService).retrieve(query.capture(), allowed.capture());
    assertThat(query.getValue().text()).isEqualTo("cache keying");
    assertThat(query.getValue().params().topK()).isEqualTo(5);
    assertThat(query.getValue().params().ref()).isEqualTo("main");
    assertThat(query.getValue().params().type()).isEqualTo("doc");
    // The ACL set is threaded straight through, so the tool cannot widen the caller's scope.
    assertThat(allowed.getValue()).containsExactlyInAnyOrder("s-a", "s-b");
  }

  @Test
  void mapsADomainFailureToAToolFailureCarryingTheSameCode() {
    when(aclFilterProvider.currentAllowedSources()).thenReturn(Set.of());
    when(retrievalService.retrieve(any(), any())).thenThrow(new SourceNotFoundException("s-x"));

    assertThatThrownBy(() -> tools.queryKnowledge("q", null, null, null))
        .isInstanceOf(ToolFailure.class)
        .satisfies(
            e -> assertThat(((ToolFailure) e).errorCode()).isEqualTo(ErrorCode.SOURCE_NOT_FOUND));
  }

  @Test
  void mapsARejectedArgumentToValidationFailedWithTheCodeInTheMessage() {
    // A blank query text is rejected by the domain with IllegalArgumentException.
    assertThatThrownBy(() -> tools.queryKnowledge("  ", null, null, null))
        .isInstanceOf(ToolFailure.class)
        .hasMessageContaining("[" + ErrorCode.VALIDATION_FAILED.name() + "]")
        .satisfies(
            e -> assertThat(((ToolFailure) e).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
  }
}
