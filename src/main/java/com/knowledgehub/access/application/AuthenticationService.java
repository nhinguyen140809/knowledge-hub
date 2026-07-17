package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.port.Authenticator;
import com.knowledgehub.access.domain.port.CredentialRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Resolves a bearer secret to a principal: hash the secret, look up the matching non-revoked
 * credential, and return its principal. An unknown or revoked secret resolves to empty, so the
 * caller fails closed. The matching credential's last-used time is recorded for audit.
 */
@Service
public class AuthenticationService implements Authenticator {

  private final CredentialRepository credentials;

  public AuthenticationService(CredentialRepository credentials) {
    this.credentials = credentials;
  }

  @Override
  public Optional<AuthenticatedPrincipal> authenticate(String bearerSecret) {
    if (bearerSecret == null || bearerSecret.isBlank()) {
      return Optional.empty();
    }
    String hash = Sha256.hex(bearerSecret);
    return credentials
        .findActivePrincipalByHash(hash)
        .map(
            principal -> {
              credentials.touchLastUsed(hash, Instant.now());
              return new AuthenticatedPrincipal(principal.principalId(), principal.role());
            });
  }
}
