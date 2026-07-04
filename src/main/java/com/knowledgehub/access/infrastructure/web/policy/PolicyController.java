package com.knowledgehub.access.infrastructure.web.policy;

import com.knowledgehub.access.application.PrincipalAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for the system-wide default policy. Admin-only; the {@code /api/v1} prefix is added by
 * WebConfig.
 */
@RestController
@RequestMapping("/admin/default-policy")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Access — Policy", description = "Read and set the default read policy")
public class PolicyController {

  private final PrincipalAdminService principals;

  public PolicyController(PrincipalAdminService principals) {
    this.principals = principals;
  }

  @GetMapping
  @Operation(summary = "Read the default policy")
  public PolicyResponse get() {
    return PolicyResponse.of(principals.defaultPolicy());
  }

  @PutMapping
  @Operation(summary = "Set the default policy")
  public PolicyResponse set(@Valid @RequestBody PolicyRequest request) {
    principals.setDefaultPolicy(request.policy());
    return PolicyResponse.of(request.policy());
  }
}
