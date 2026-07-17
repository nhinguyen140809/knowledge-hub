package com.knowledgehub.access.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.access.domain.port.CredentialRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTests {

  private final CredentialRepository credentials =
      org.mockito.Mockito.mock(CredentialRepository.class);
  private final AuthenticationService service = new AuthenticationService(credentials);

  @Test
  void resolvesAPrincipalAndRecordsUseForAValidSecret() {
    when(credentials.findActivePrincipalByHash(any()))
        .thenReturn(Optional.of(new Principal("p1", PrincipalType.SUBJECT, Role.ADMIN)));

    Optional<AuthenticatedPrincipal> result = service.authenticate("a-secret");

    assertThat(result).isPresent();
    assertThat(result.get().principalId()).isEqualTo("p1");
    assertThat(result.get().isAdmin()).isTrue();
    verify(credentials).touchLastUsed(eq(Sha256.hex("a-secret")), any());
  }

  @Test
  void rejectsAnUnknownOrRevokedSecret() {
    when(credentials.findActivePrincipalByHash(any())).thenReturn(Optional.empty());

    assertThat(service.authenticate("bad")).isEmpty();
    verify(credentials, never()).touchLastUsed(any(), any());
  }

  @Test
  void rejectsABlankSecretWithoutTouchingTheStore() {
    assertThat(service.authenticate("  ")).isEmpty();
    assertThat(service.authenticate(null)).isEmpty();
    verify(credentials, never()).findActivePrincipalByHash(any());
  }
}
