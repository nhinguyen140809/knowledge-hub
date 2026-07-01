package com.knowledgehub.access.application;

/**
 * The result of issuing a credential. The {@code secret} is the raw bearer token, returned exactly
 * once and never stored; losing it means issuing a new credential.
 *
 * @param credentialId the new credential's id (safe to keep and reference)
 * @param secret the raw secret, shown this once only
 */
public record IssuedCredential(String credentialId, String secret) {}
