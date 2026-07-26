package com.knowledgehub.access.infrastructure.web.source;

import com.knowledgehub.access.application.SourcePrincipalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for source-centric access inspection: which principals can read a given source, the
 * inverse of the principal-centric effective-permissions view. Admin-only; the {@code /api/v1}
 * prefix is added by WebConfig.
 */
@RestController
@RequestMapping("/admin/sources")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Access — Sources", description = "Which principals can read a source")
public class SourcePrincipalsController {

  private final SourcePrincipalsService service;

  public SourcePrincipalsController(SourcePrincipalsService service) {
    this.service = service;
  }

  @GetMapping("/{id}/principals")
  @Operation(summary = "Every principal that can read a source, with its access origin")
  public SourcePrincipalsResponse principals(@PathVariable String id) {
    return SourcePrincipalsResponse.from(service.resolve(id));
  }
}
