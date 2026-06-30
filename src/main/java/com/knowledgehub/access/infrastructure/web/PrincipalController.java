package com.knowledgehub.access.infrastructure.web;

import com.knowledgehub.access.application.CredentialService;
import com.knowledgehub.access.application.PrincipalAdminService;
import com.knowledgehub.access.domain.Principal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Admin API for principals, group membership, credential issuing/listing and effective-permission
 * inspection. Admin-only; the {@code /api/v1} prefix is added by WebConfig.
 */
@RestController
@RequestMapping("/admin/principals")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Access — Principals", description = "Manage principals, groups and their credentials")
public class PrincipalController {

  private final PrincipalAdminService principals;
  private final CredentialService credentials;

  public PrincipalController(
      PrincipalAdminService principals, CredentialService credentials) {
    this.principals = principals;
    this.credentials = credentials;
  }

  @PostMapping
  @Operation(summary = "Create a principal (subject or group)")
  public ResponseEntity<PrincipalResponse> create(
      @Valid @RequestBody CreatePrincipalRequest request) {
    Principal created = principals.create(request.principalId(), request.type(), request.role());
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.principalId())
            .toUri();
    return ResponseEntity.created(location).body(PrincipalResponse.from(created));
  }

  @GetMapping
  @Operation(summary = "List all principals")
  public List<PrincipalResponse> list() {
    return principals.list().stream().map(PrincipalResponse::from).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a principal")
  public PrincipalResponse get(@PathVariable String id) {
    return PrincipalResponse.from(principals.get(id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a principal")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    principals.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/members")
  @Operation(summary = "List a group's direct members")
  public List<String> members(@PathVariable String id) {
    return principals.members(id);
  }

  @PostMapping("/{id}/members")
  @Operation(summary = "Add a member to a group")
  public ResponseEntity<Void> addMember(
      @PathVariable String id, @Valid @RequestBody AddMemberRequest request) {
    principals.addMember(id, request.memberId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/members/{memberId}")
  @Operation(summary = "Remove a member from a group")
  public ResponseEntity<Void> removeMember(
      @PathVariable String id, @PathVariable String memberId) {
    principals.removeMember(id, memberId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/credentials")
  @Operation(summary = "Issue a credential (returns the secret once)")
  public IssuedCredentialResponse issueCredential(@PathVariable String id) {
    return IssuedCredentialResponse.from(credentials.issue(id));
  }

  @GetMapping("/{id}/credentials")
  @Operation(summary = "List a principal's credentials (metadata only)")
  public List<CredentialResponse> listCredentials(@PathVariable String id) {
    return credentials.list(id).stream().map(CredentialResponse::from).toList();
  }

  @GetMapping("/{id}/effective-permissions")
  @Operation(summary = "Resolve a principal's effective read access")
  public EffectivePermissionsResponse effectivePermissions(@PathVariable String id) {
    return EffectivePermissionsResponse.from(principals.effectivePermissions(id));
  }
}
