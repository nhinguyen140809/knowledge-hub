package com.knowledgehub.access.domain;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/** Thrown when creating a principal whose id already exists. Maps to HTTP 409. */
public class DuplicatePrincipalException extends DomainException {

  public DuplicatePrincipalException(String principalId) {
    super(ErrorCode.DUPLICATE_PRINCIPAL, "Principal already exists: " + principalId);
  }
}
