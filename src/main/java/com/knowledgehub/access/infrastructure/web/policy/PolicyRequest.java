package com.knowledgehub.access.infrastructure.web.policy;

import com.knowledgehub.access.domain.DefaultPolicy;
import jakarta.validation.constraints.NotNull;

/** Request to set the system-wide default policy. */
public record PolicyRequest(@NotNull DefaultPolicy policy) {}
