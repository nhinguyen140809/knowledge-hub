package com.knowledgehub.access.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.Authorizer;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.shared.config.AppProperties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AclFilterProviderTests {

  private final AtomicInteger resolutions = new AtomicInteger();

  private final Authorizer counting =
      new Authorizer() {
        @Override
        public Set<String> readableSources(AuthenticatedPrincipal principal) {
          resolutions.incrementAndGet();
          return Set.of(principal.principalId() + "-src");
        }

        @Override
        public boolean isAdmin(AuthenticatedPrincipal principal) {
          return principal.isAdmin();
        }
      };

  private final AclFilterProvider provider =
      new AclFilterProvider(counting, new AppProperties(null, null, null, null, null, null));

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static void authenticateAs(String principalId) {
    var principal = new AuthenticatedPrincipal(principalId, Role.MEMBER);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
  }

  @Test
  void resolvesOncePerPrincipalThenServesFromCache() {
    authenticateAs("alice");

    Set<String> first = provider.currentAllowedSources();
    Set<String> second = provider.currentAllowedSources();

    assertThat(first).containsExactly("alice-src");
    assertThat(second).isEqualTo(first);
    assertThat(resolutions.get()).isEqualTo(1); // the second call hit the cache
  }

  @Test
  void doesNotShareCacheAcrossPrincipals() {
    authenticateAs("alice");
    Set<String> alice = provider.currentAllowedSources();

    authenticateAs("bob");
    Set<String> bob = provider.currentAllowedSources();

    // Keyed by principal id, so one caller never gets another's readable set.
    assertThat(alice).containsExactly("alice-src");
    assertThat(bob).containsExactly("bob-src");
    assertThat(resolutions.get()).isEqualTo(2);
  }

  @Test
  void returnsNothingWhenUnauthenticated() {
    assertThat(provider.currentAllowedSources()).isEmpty();
    assertThat(resolutions.get()).isZero(); // fail-closed, no resolution attempted
  }
}
