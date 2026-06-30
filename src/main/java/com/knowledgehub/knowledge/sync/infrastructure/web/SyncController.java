package com.knowledgehub.knowledge.sync.infrastructure.web;

import com.knowledgehub.knowledge.sync.application.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST API to sync a source and read its freshness. The {@code /api/v1} prefix is added by
 * WebConfig. Triggering a sync is idempotent: re-syncing an unchanged source is a no-op.
 */
@RestController
@RequestMapping("/admin/sources/{id}")
@Tag(name = "Sync", description = "Keep a source's index consistent with the source")
public class SyncController {

  private final SyncService syncService;

  public SyncController(SyncService syncService) {
    this.syncService = syncService;
  }

  @PostMapping("/sync")
  @Operation(summary = "Sync a source incrementally (idempotent)")
  public SyncResponse sync(@PathVariable String id) {
    return SyncResponse.from(syncService.sync(id));
  }

  @GetMapping("/status")
  @Operation(summary = "Read a source's index freshness")
  public SourceStatusResponse status(@PathVariable String id) {
    return SourceStatusResponse.from(id, syncService.freshnessOf(id));
  }
}
