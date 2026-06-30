package com.knowledgehub.access.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.GrantRepository;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.access.domain.SystemConfigRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTests {

  private final GrantRepository grants = mock(GrantRepository.class);
  private final SystemConfigRepository systemConfig = mock(SystemConfigRepository.class);
  private final SourceRepository sources = mock(SourceRepository.class);
  private final AuthorizationService service =
      new AuthorizationService(grants, systemConfig, sources);

  private final AuthenticatedPrincipal principal = new AuthenticatedPrincipal("p1", Role.MEMBER);

  @Test
  void denyPolicyReadsOnlyTheUnionOfGrantedSources() {
    when(systemConfig.defaultPolicy()).thenReturn(DefaultPolicy.DENY);
    when(grants.readableSourcesFor("p1")).thenReturn(Set.of("s-a", "s-b"));

    assertThat(service.readableSources(principal)).containsExactlyInAnyOrder("s-a", "s-b");
  }

  @Test
  void allowPolicyReadsEverythingExceptRestrictedButKeepsOwnGrants() {
    when(systemConfig.defaultPolicy()).thenReturn(DefaultPolicy.ALLOW);
    when(sources.findAll()).thenReturn(List.of(source("s-a"), source("s-b"), source("s-c")));
    // s-a and s-b appear in some grant, so they are restricted.
    when(grants.allGrantedSources()).thenReturn(Set.of("s-a", "s-b"));
    // The principal itself is granted s-a, so it still reads it.
    when(grants.readableSourcesFor("p1")).thenReturn(Set.of("s-a"));

    assertThat(service.readableSources(principal)).containsExactlyInAnyOrder("s-c", "s-a");
  }

  private static Source source(String id) {
    return new Source(id, SourceType.FS, "/" + id, null, List.of(), List.of());
  }
}
