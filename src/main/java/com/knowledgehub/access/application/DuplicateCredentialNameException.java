package com.knowledgehub.access.application;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/**
 * Thrown when issuing a credential whose name is already used by an active credential of the same
 * principal. Maps to HTTP 409.
 */
public class DuplicateCredentialNameException extends DomainException {

  public DuplicateCredentialNameException(String principalId, String name) {
    super(
        ErrorCode.DUPLICATE_CREDENTIAL_NAME,
        "Credential name '" + name + "' already in use for principal: " + principalId);
  }
}
