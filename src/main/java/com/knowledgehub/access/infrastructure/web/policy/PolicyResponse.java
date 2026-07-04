package com.knowledgehub.access.infrastructure.web.policy;

import com.knowledgehub.access.domain.DefaultPolicy;

/** JSON view of the system-wide default policy. */
public record PolicyResponse(DefaultPolicy policy) {

  static PolicyResponse of(DefaultPolicy policy) {
    return new PolicyResponse(policy);
  }
}
