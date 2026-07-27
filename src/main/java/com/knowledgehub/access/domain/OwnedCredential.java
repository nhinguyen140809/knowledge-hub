package com.knowledgehub.access.domain;

/**
 * A credential paired with the id of the principal it belongs to — used where the principal isn't
 * already implied by context, such as a cross-principal credential list.
 *
 * @param principalId the owning principal
 * @param credential the credential metadata
 */
public record OwnedCredential(String principalId, Credential credential) {}
