package com.knowledgehub.access.application;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/** Thrown when an operation references a principal id that does not exist. Maps to HTTP 404. */
public class PrincipalNotFoundException extends DomainException {

  public PrincipalNotFoundException(String principalId) {
    super(ErrorCode.PRINCIPAL_NOT_FOUND, "Principal not found: " + principalId);
  }
}
