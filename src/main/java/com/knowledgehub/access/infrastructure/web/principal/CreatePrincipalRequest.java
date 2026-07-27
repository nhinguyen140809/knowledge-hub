package com.knowledgehub.access.infrastructure.web.principal;

import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.shared.validation.IdFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request to create a principal.
 *
 * @param principalId the new principal's id
 * @param type subject or group
 * @param role admin or member
 * @param parentGroupId group to create the principal directly inside, atomically; null for a
 *     top-level principal
 */
public record CreatePrincipalRequest(
    @NotBlank @Pattern(regexp = IdFormat.PATTERN, message = IdFormat.MESSAGE) String principalId,
    @NotNull PrincipalType type,
    @NotNull Role role,
    String parentGroupId) {}
