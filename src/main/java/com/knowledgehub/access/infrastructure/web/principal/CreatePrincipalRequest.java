package com.knowledgehub.access.infrastructure.web.principal;

import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create a principal.
 *
 * @param principalId the new principal's id
 * @param type subject or group
 * @param role admin or member
 */
public record CreatePrincipalRequest(
    @NotBlank String principalId, @NotNull PrincipalType type, @NotNull Role role) {}
