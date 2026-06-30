package com.knowledgehub.access.infrastructure.web;

import com.knowledgehub.access.domain.Credential;
import java.time.Instant;

/** JSON credential metadata for management/audit. Never carries the secret or its hash. */
public record CredentialResponse(
    String credentialId, boolean revoked, Instant createdAt, Instant lastUsedAt) {

  static CredentialResponse from(Credential credential) {
    return new CredentialResponse(
        credential.credentialId(),
        credential.revoked(),
        credential.createdAt(),
        credential.lastUsedAt());
  }
}
