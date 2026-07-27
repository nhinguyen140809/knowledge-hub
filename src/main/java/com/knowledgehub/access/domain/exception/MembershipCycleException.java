package com.knowledgehub.access.domain.exception;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/** Thrown when adding a member to a group would close a membership cycle. Maps to HTTP 409. */
public class MembershipCycleException extends DomainException {

  public MembershipCycleException(String groupId, String memberId) {
    super(
        ErrorCode.MEMBERSHIP_CYCLE,
        "Adding '%s' to '%s' would create a membership cycle".formatted(memberId, groupId));
  }
}
