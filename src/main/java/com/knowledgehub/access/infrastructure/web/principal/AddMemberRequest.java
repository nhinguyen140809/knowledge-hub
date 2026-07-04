package com.knowledgehub.access.infrastructure.web.principal;

import jakarta.validation.constraints.NotBlank;

/** Request to add a principal to a group. */
public record AddMemberRequest(@NotBlank String memberId) {}
