package com.knowledgehub.access.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Request to grant or revoke a principal's read access to a set of sources. */
public record GrantRequest(@NotBlank String principalId, @NotEmpty List<String> sourceIds) {}
