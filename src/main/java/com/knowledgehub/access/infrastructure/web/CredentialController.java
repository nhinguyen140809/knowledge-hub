package com.knowledgehub.access.infrastructure.web;

import com.knowledgehub.access.application.CredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API to revoke a credential by id. Revoke is a soft-delete; the next request using that
 * credential fails authentication. Admin-only; the {@code /api/v1} prefix is added by WebConfig.
 */
@RestController
@RequestMapping("/admin/credentials")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Access — Credentials", description = "Revoke credentials")
public class CredentialController {

  private final CredentialService credentials;

  public CredentialController(CredentialService credentials) {
    this.credentials = credentials;
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Revoke a credential")
  public ResponseEntity<Void> revoke(@PathVariable String id) {
    credentials.revoke(id);
    return ResponseEntity.noContent().build();
  }
}
