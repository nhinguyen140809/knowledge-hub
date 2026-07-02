package com.knowledgehub.access.infrastructure.web;

import com.knowledgehub.access.application.IssuedCredential;

/**
 * JSON response for issuing a credential. Carries the raw {@code secret} — returned this once only;
 * it is never stored and cannot be retrieved again.
 */
public record IssuedCredentialResponse(String credentialId, String name, String secret) {

  static IssuedCredentialResponse from(IssuedCredential issued) {
    return new IssuedCredentialResponse(issued.credentialId(), issued.name(), issued.secret());
  }
}
