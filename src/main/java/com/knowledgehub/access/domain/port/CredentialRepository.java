package com.knowledgehub.access.domain.port;

import com.knowledgehub.access.domain.Credential;
import com.knowledgehub.access.domain.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Stores credentials as the hash of their secret, attached to one principal. Authentication looks a
 * principal up by hash; the raw secret never reaches this layer.
 */
public interface CredentialRepository {

  /** Persists a credential (by hash) and attaches it to a principal. */
  void save(String credentialId, String principalId, String name, String hash, Instant createdAt);

  /** Whether the principal already has an active (non-revoked) credential with this name. */
  boolean existsActiveByPrincipalAndName(String principalId, String name);

  /**
   * Resolves the active (non-revoked) principal owning the credential with this hash, or empty if
   * none — an unknown or revoked hash authenticates nothing (fail-closed).
   */
  Optional<Principal> findActivePrincipalByHash(String hash);

  /** Records that the credential with this hash just authenticated a request. */
  void touchLastUsed(String hash, Instant when);

  /** Marks a credential revoked (soft-delete; the node is kept for audit). */
  void revoke(String credentialId);

  /** Lists a principal's credentials as metadata only (no secret, no hash). */
  List<Credential> listByPrincipal(String principalId);

  /** Hard-deletes revoked credentials whose creation predates the cutoff; returns how many. */
  int purgeRevokedBefore(Instant cutoff);
}
