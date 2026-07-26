package com.knowledgehub.access.infrastructure.web.principal;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to atomically move a principal between groups.
 *
 * @param fromGroupId the membership edge to remove; null when the principal is not in a group yet
 * @param toGroupId the group to add the principal to
 */
public record MoveRequest(String fromGroupId, @NotBlank String toGroupId) {}
