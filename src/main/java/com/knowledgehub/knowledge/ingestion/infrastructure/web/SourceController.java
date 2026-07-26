package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.knowledgehub.knowledge.ingestion.application.SourceService;
import com.knowledgehub.knowledge.ingestion.application.port.SourceFreshness;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
  private final SourceFreshness sourceFreshness;

  public SourceController(SourceService sourceService, SourceFreshness sourceFreshness) {
    this.sourceService = sourceService;
    this.sourceFreshness = sourceFreshness;
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
    // A freshly registered source has never been synced, so its updatedAt is null.
    return ResponseEntity.created(location).body(SourceResponse.from(created, null));
  }

  @GetMapping("/summary")
  @Operation(summary = "At-a-glance freshness roll-up across every source")
  public SourceSummaryResponse summary() {
    return SourceSummaryResponse.from(sourceService.summary());
  }

  @GetMapping
  @Operation(summary = "List all configured sources")
  public List<SourceResponse> list() {
    List<Source> sources = sourceService.list();
    Map<String, Instant> updatedAt =
        sourceFreshness.lastIndexedAt(sources.stream().map(Source::sourceId).toList());
    return sources.stream()
        .map(source -> SourceResponse.from(source, updatedAt.get(source.sourceId())))
        .toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a source by id")
  public SourceResponse get(@PathVariable String id) {
    return SourceResponse.from(sourceService.get(id), updatedAtOf(id));
  }

  @PatchMapping("/{id}")
  @Operation(
      summary = "Update a source's ref, globs, name and description (partial update)",
      description =
          """
          Partial update — only the fields you send change. For each field: an omitted field \
          (or null) keeps its current value, an empty array [] clears a glob list and a non-empty \
          array replaces it, and a blank name/description clears it. The id, type and location \
          cannot be changed here. Only glob changes affect the index, and even then it is not \
          touched — run a sync afterwards to apply them.""")
  public SourceResponse update(
      @PathVariable String id, @Valid @RequestBody UpdateSourceRequest request) {
    Source updated =
        sourceService.update(
            id,
            request.ref(),
            request.include(),
            request.ignore(),
            request.name(),
            request.description());
    return SourceResponse.from(updated, updatedAtOf(id));
  }

  /** The single source's last-sync instant, or {@code null} when it has never been synced. */
  private Instant updatedAtOf(String sourceId) {
    return sourceFreshness.lastIndexedAt(List.of(sourceId)).get(sourceId);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Remove a source")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    sourceService.remove(id);
    return ResponseEntity.noContent().build();
  }
}
