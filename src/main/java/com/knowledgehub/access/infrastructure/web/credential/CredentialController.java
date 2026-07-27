package com.knowledgehub.access.infrastructure.web.credential;

import com.knowledgehub.access.application.CredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API to list credentials across every principal and revoke one by id. Revoke is a
 * soft-delete; the next request using that credential fails authentication. Admin-only; the {@code
 * /api/v1} prefix is added by WebConfig.
 */
@RestController
@RequestMapping("/admin/credentials")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Access — Credentials", description = "List and revoke credentials")
public class CredentialController {

  private final CredentialService credentials;

  public CredentialController(CredentialService credentials) {
    this.credentials = credentials;
  }

  @GetMapping
  @Operation(summary = "List every credential across every principal")
  public List<GlobalCredentialResponse> list() {
    return credentials.listAll().stream().map(GlobalCredentialResponse::from).toList();
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Revoke a credential")
  public ResponseEntity<Void> revoke(@PathVariable String id) {
    credentials.revoke(id);
    return ResponseEntity.noContent().build();
  }
}
