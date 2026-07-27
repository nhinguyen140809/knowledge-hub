package com.knowledgehub.access.domain.exception;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/**
 * Thrown when granting a source to an ADMIN principal — its role already reads everything, so the
 * grant would be dead config the admin panels then have to explain. Maps to HTTP 409.
 */
public class AdminGrantException extends DomainException {

  public AdminGrantException(String principalId) {
    super(ErrorCode.ADMIN_GRANT, "Admin principal already reads everything: " + principalId);
  }
}
