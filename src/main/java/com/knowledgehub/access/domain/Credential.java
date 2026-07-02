package com.knowledgehub.access.domain;

import java.time.Instant;

/**
 * Metadata about a credential, for management and audit. Deliberately carries no secret and no
 * hash: the raw secret is shown once at issue and never stored, and the hash is an authentication
 * detail that must never leave the system.
 *
 * @param credentialId stable unique id
 * @param name human-readable label, unique among a principal's active credentials, so an admin can
 *     tell credentials apart when listing or revoking
 * @param revoked whether the credential has been revoked (a revoked credential authenticates
 *     nothing)
 * @param createdAt when it was issued
 * @param lastUsedAt when it last authenticated a request, or {@code null} if never used
 */
public record Credential(
    String credentialId, String name, boolean revoked, Instant createdAt, Instant lastUsedAt) {}
