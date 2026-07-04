package com.knowledgehub.access.infrastructure.web.grant;

import com.knowledgehub.access.application.PrincipalAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin API for read grants. Admin-only; the {@code /api/v1} prefix is added by WebConfig. */
@RestController
@RequestMapping("/admin/grants")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Access — Grants", description = "Grant and revoke read access to sources")
public class GrantController {

  private final PrincipalAdminService principals;

  public GrantController(PrincipalAdminService principals) {
    this.principals = principals;
  }

  @PostMapping
  @Operation(summary = "Grant a principal read access to sources")
  public ResponseEntity<Void> grant(@Valid @RequestBody GrantRequest request) {
    principals.grant(request.principalId(), request.sourceIds());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/revoke")
  @Operation(summary = "Revoke a principal's read access to sources")
  public ResponseEntity<Void> revoke(@Valid @RequestBody GrantRequest request) {
    principals.revokeGrant(request.principalId(), request.sourceIds());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{principalId}")
  @Operation(summary = "List a principal's directly granted sources")
  public List<String> list(@PathVariable String principalId) {
    return principals.grantedSources(principalId);
  }
}
