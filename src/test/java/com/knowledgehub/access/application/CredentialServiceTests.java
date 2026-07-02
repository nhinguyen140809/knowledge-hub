package com.knowledgehub.access.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.access.domain.CredentialRepository;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalRepository;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CredentialServiceTests {

  private final CredentialRepository credentials = mock(CredentialRepository.class);
  private final PrincipalRepository principals = mock(PrincipalRepository.class);
  private final CredentialService service = new CredentialService(credentials, principals);

  @Test
  void issuingStoresOnlyTheHashAndReturnsTheSecretOnce() {
    when(principals.findById("p1"))
        .thenReturn(Optional.of(new Principal("p1", PrincipalType.SUBJECT, Role.MEMBER)));

    IssuedCredential issued = service.issue("p1", "laptop");

    assertThat(issued.secret()).isNotBlank();
    assertThat(issued.name()).isEqualTo("laptop");
    ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
    verify(credentials)
        .save(eq(issued.credentialId()), eq("p1"), eq("laptop"), hash.capture(), any());
    // What is stored is the hash of the secret, never the secret itself.
    assertThat(hash.getValue()).isEqualTo(Sha256.hex(issued.secret()));
    assertThat(hash.getValue()).isNotEqualTo(issued.secret());
  }

  @Test
  void issuingForAnUnknownPrincipalThrows() {
    when(principals.findById("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.issue("nope", "laptop"))
        .isInstanceOf(PrincipalNotFoundException.class);
  }

  @Test
  void issuingADuplicateNameForThePrincipalThrows() {
    when(principals.findById("p1"))
        .thenReturn(Optional.of(new Principal("p1", PrincipalType.SUBJECT, Role.MEMBER)));
    when(credentials.existsActiveByPrincipalAndName("p1", "laptop")).thenReturn(true);

    assertThatThrownBy(() -> service.issue("p1", "laptop"))
        .isInstanceOf(DuplicateCredentialNameException.class);
  }

  @Test
  void revokeDelegatesToTheRepository() {
    service.revoke("cred-1");
    verify(credentials).revoke("cred-1");
  }
}
