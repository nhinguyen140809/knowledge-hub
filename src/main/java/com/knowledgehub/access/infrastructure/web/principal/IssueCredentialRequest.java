package com.knowledgehub.access.infrastructure.web.principal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to issue a credential. The {@code name} is a human-readable label, unique among the
 * principal's active credentials, so an admin can tell them apart when listing or revoking.
 *
 * @param name label such as {@code laptop} or {@code ci-pipeline}
 */
public record IssueCredentialRequest(
    @NotBlank
        @Size(max = 100)
        @Pattern(
            regexp = "[A-Za-z0-9][A-Za-z0-9 ._-]*",
            message = "must start alphanumeric and contain only letters, digits, space, . _ -")
        String name) {}
