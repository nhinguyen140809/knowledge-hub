package com.knowledgehub.access.domain.exception;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/**
 * Thrown when deleting the last remaining ADMIN principal — doing so would leave nobody able to
 * administer the system until the next restart re-seeds a bootstrap admin. Maps to HTTP 409.
 */
public class LastAdminException extends DomainException {

  public LastAdminException(String principalId) {
    super(ErrorCode.LAST_ADMIN, "Cannot delete the last remaining admin: " + principalId);
  }
}
