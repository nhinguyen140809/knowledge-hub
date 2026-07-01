package com.knowledgehub.knowledge.sync.infrastructure.mcp;

import com.knowledgehub.knowledge.sync.application.SyncService;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import com.knowledgehub.shared.error.ToolErrors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * MCP tool letting an admin agent bring a source's index up to date before querying. It is an
 * inbound adapter over the same {@link SyncService} the REST controller calls — no sync logic lives
 * here. Syncing is an administrative operation, so the tool carries the same admin check the REST
 * sync endpoint does. The call is idempotent: re-syncing an unchanged source is a no-op.
 *
 * <p>Named to avoid the {@code syncTools} bean the MCP server auto-configuration already defines.
 */
@Component
public class SyncSourceTools {

  private final SyncService syncService;

  public SyncSourceTools(SyncService syncService) {
    this.syncService = syncService;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Tool(
      name = "sync_source",
      description =
          "Bring a knowledge source's index up to date with its origin, then report what changed. "
              + "Call this before querying when you need the freshest results. Idempotent: syncing "
              + "an unchanged source does nothing. Returns counts of indexed, re-indexed, evicted, "
              + "and skipped files.")
  public SyncResult syncSource(
      @ToolParam(description = "The id of the source to sync") String sourceId) {
    return ToolErrors.mapped(() -> syncService.sync(sourceId));
  }
}
