package com.knowledgehub.access.domain.exception;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/**
 * Thrown when adding an ADMIN principal to a group. Membership exists to confer access, and an
 * admin's access is already total by role, so the edge could only mislead. Maps to HTTP 409.
 */
public class AdminMembershipException extends DomainException {

  public AdminMembershipException(String principalId) {
    super(ErrorCode.ADMIN_MEMBERSHIP, "Admin principal cannot join a group: " + principalId);
  }
}
