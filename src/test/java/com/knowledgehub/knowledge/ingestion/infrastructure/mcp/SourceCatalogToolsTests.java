package com.knowledgehub.knowledge.ingestion.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.access.infrastructure.security.AclFilterProvider;
import com.knowledgehub.knowledge.ingestion.application.SourceService;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SourceCatalogToolsTests {

  private final SourceService sourceService = mock(SourceService.class);
  private final AclFilterProvider aclFilterProvider = mock(AclFilterProvider.class);
  private final SourceCatalogTools tools = new SourceCatalogTools(sourceService, aclFilterProvider);

  @Test
  void listsOnlyTheSourcesTheCallerMayRead() {
    when(sourceService.list()).thenReturn(List.of(git("s-a", "main"), fs("s-b"), git("s-c", null)));
    when(aclFilterProvider.currentAllowedSources()).thenReturn(Set.of("s-a", "s-b"));

    List<ReadableSourceResponse> sources = tools.listSources();

    assertThat(sources)
        .containsExactly(
            new ReadableSourceResponse("s-a", SourceType.GIT, "main"),
            new ReadableSourceResponse("s-b", SourceType.FS, null));
  }

  @Test
  void anEmptyReadableSetYieldsAnEmptyList() {
    when(sourceService.list()).thenReturn(List.of(git("s-a", "main")));
    when(aclFilterProvider.currentAllowedSources()).thenReturn(Set.of());

    assertThat(tools.listSources()).isEmpty();
  }

  private static Source git(String id, String ref) {
    return new Source(
        id, SourceType.GIT, "git@example.com:" + id + ".git", ref, List.of(), List.of());
  }

  private static Source fs(String id) {
    return new Source(id, SourceType.FS, "/data/" + id, null, List.of(), List.of());
  }
}
