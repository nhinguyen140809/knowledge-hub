package com.knowledgehub.access.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.Authorizer;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.shared.config.AppProperties;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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

  @Test
  void picksUpAGrantChangeWithinTheCacheTtl() {
    // A runtime ACL change: the authorizer starts granting one source, then a second is added.
    AtomicReference<Set<String>> readable = new AtomicReference<>(Set.of("docs"));
    Authorizer changing =
        new Authorizer() {
          @Override
          public Set<String> readableSources(AuthenticatedPrincipal principal) {
            return readable.get();
          }

          @Override
          public boolean isAdmin(AuthenticatedPrincipal principal) {
            return principal.isAdmin();
          }
        };
    // A hand-driven clock so TTL expiry is deterministic (no sleeps); default aclCacheTtl is 5s.
    AtomicLong clock = new AtomicLong();
    AclFilterProvider ttlProvider =
        new AclFilterProvider(
            changing, new AppProperties(null, null, null, null, null, null), clock::get);
    authenticateAs("alice");

    assertThat(ttlProvider.currentAllowedSources()).containsExactly("docs");

    readable.set(Set.of("docs", "shared-lib")); // admin grants a new source at runtime

    // Still within the TTL: the cached set is served, so the change is not visible yet.
    assertThat(ttlProvider.currentAllowedSources()).containsExactly("docs");

    // Past the TTL (<= 5s bound): the next request re-resolves and reflects the change, no restart.
    clock.addAndGet(Duration.ofSeconds(6).toNanos());
    assertThat(ttlProvider.currentAllowedSources()).containsExactlyInAnyOrder("docs", "shared-lib");
  }
}
