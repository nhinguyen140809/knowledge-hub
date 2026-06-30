package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.Credential;
import com.knowledgehub.access.domain.CredentialRepository;
import com.knowledgehub.access.domain.PrincipalRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Issues and revokes credentials. Issuing generates a 256-bit random secret, stores only its hash,
 * and returns the raw secret once; the secret is never logged or persisted. Revoking is a
 * soft-delete so the audit trail survives — a revoked credential simply stops authenticating.
 */
@Service
public class CredentialService {

  private static final Logger log = LoggerFactory.getLogger(CredentialService.class);
  private static final int SECRET_BYTES = 32;

  private final CredentialRepository credentials;
  private final PrincipalRepository principals;
  private final SecureRandom random = new SecureRandom();

  public CredentialService(CredentialRepository credentials, PrincipalRepository principals) {
    this.credentials = credentials;
    this.principals = principals;
  }

  /** Issues a new credential for a principal and returns the raw secret once. */
  public IssuedCredential issue(String principalId) {
    if (principals.findById(principalId).isEmpty()) {
      throw new PrincipalNotFoundException(principalId);
    }
    byte[] bytes = new byte[SECRET_BYTES];
    random.nextBytes(bytes);
    String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String credentialId = UUID.randomUUID().toString();
    credentials.save(credentialId, principalId, Sha256.hex(secret), Instant.now());
    log.info("Issued credential {} for principal {}", credentialId, principalId);
    return new IssuedCredential(credentialId, secret);
  }

  /** Revokes a credential; the next request using it fails authentication. */
  public void revoke(String credentialId) {
    credentials.revoke(credentialId);
    log.info("Revoked credential {}", credentialId);
  }

  /** Lists a principal's credentials as metadata only (never the secret or hash). */
  public List<Credential> list(String principalId) {
    if (principals.findById(principalId).isEmpty()) {
      throw new PrincipalNotFoundException(principalId);
    }
    return credentials.listByPrincipal(principalId);
  }
}
