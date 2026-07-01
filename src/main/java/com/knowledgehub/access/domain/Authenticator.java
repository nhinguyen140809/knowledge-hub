package com.knowledgehub.access.domain;

import java.util.Optional;

/** Resolves a bearer secret to an authenticated principal, or empty when it is invalid/revoked. */
public interface Authenticator {

  Optional<AuthenticatedPrincipal> authenticate(String bearerSecret);
}
