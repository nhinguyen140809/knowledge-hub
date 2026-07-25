package com.knowledgehub.access.infrastructure.web.principal;

import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request to create a principal.
 *
 * @param principalId the new principal's id
 * @param type subject or group
 * @param role admin or member
 */
public record CreatePrincipalRequest(
    @NotBlank
        @Pattern(
            regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "must be lowercase letters, numbers and hyphens only")
        String principalId,
    @NotNull PrincipalType type,
    @NotNull Role role) {}
