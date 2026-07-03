package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.knowledgehub.knowledge.ingestion.application.SourceService;
import com.knowledgehub.knowledge.ingestion.domain.Source;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Admin REST API to manage configured sources. The {@code /api/v1} prefix is added by WebConfig.
 */
@RestController
@RequestMapping("/admin/sources")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Sources", description = "Admin management of ingestion sources")
public class SourceController {

  private final SourceService sourceService;

  public SourceController(SourceService sourceService) {
    this.sourceService = sourceService;
  }

  @PostMapping
  @Operation(summary = "Register a new source")
  public ResponseEntity<SourceResponse> create(@Valid @RequestBody CreateSourceRequest request) {
    Source created = sourceService.register(request.toSpec());
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.sourceId())
            .toUri();
    return ResponseEntity.created(location).body(SourceResponse.from(created));
  }

  @GetMapping
  @Operation(summary = "List all configured sources")
  public List<SourceResponse> list() {
    return sourceService.list().stream().map(SourceResponse::from).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a source by id")
  public SourceResponse get(@PathVariable String id) {
    return SourceResponse.from(sourceService.get(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a source's ref and include/ignore globs")
  public SourceResponse update(
      @PathVariable String id, @Valid @RequestBody UpdateSourceRequest request) {
    Source updated = sourceService.update(id, request.ref(), request.include(), request.ignore());
    return SourceResponse.from(updated);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Remove a source")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    sourceService.remove(id);
    return ResponseEntity.noContent().build();
  }
}
