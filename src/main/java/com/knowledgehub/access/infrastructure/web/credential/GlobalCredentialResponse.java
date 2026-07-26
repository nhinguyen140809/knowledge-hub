package com.knowledgehub.access.infrastructure.web.credential;

import com.knowledgehub.access.domain.Credential;
import com.knowledgehub.access.domain.OwnedCredential;
import java.time.Instant;

/**
 * JSON credential metadata for the cross-principal list — the same fields as a principal-scoped
 * {@code CredentialResponse}, plus {@code principalId} since the owner isn't implied by the URL
 * here.
 */
public record GlobalCredentialResponse(
    String principalId,
    String credentialId,
    String name,
    boolean revoked,
    Instant createdAt,
    Instant lastUsedAt) {

  static GlobalCredentialResponse from(OwnedCredential owned) {
    Credential credential = owned.credential();
    return new GlobalCredentialResponse(
        owned.principalId(),
        credential.credentialId(),
        credential.name(),
        credential.revoked(),
        credential.createdAt(),
        credential.lastUsedAt());
  }
}
