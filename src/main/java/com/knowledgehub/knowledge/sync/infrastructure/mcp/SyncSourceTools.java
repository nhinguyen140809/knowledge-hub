package com.knowledgehub.knowledge.sync.infrastructure.mcp;

import com.knowledgehub.knowledge.sync.application.SyncService;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import com.knowledgehub.knowledge.sync.infrastructure.web.SourceStatusResponse;
import com.knowledgehub.shared.error.ToolErrors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * MCP tools for keeping knowledge fresh: {@code source_status} lets any authenticated agent check
 * how up to date a source's index is, and {@code sync_source} lets an admin agent bring it up to
 * date before querying. Both are inbound adapters over the same {@link SyncService} the REST
 * controllers call — no sync logic lives here. Only syncing is administrative, so only it carries
 * the admin check; with {@code list_sources} they support the loop <em>discover sources → check
 * freshness → (if stale) sync → query</em>.
 *
 * <p>Named to avoid the {@code syncTools} bean the MCP server auto-configuration already defines.
 */
@Component
public class SyncSourceTools {

  private final SyncService syncService;

  public SyncSourceTools(SyncService syncService) {
    this.syncService = syncService;
  }

  @Tool(
      name = "source_status",
      description =
          "Check how fresh a knowledge source's index is: whether it has ever been synced, when it "
              + "was last synced, and (for a git source) at which ref and commit. Call this to "
              + "decide whether to sync before querying. Read-only and available to every "
              + "authenticated agent.")
  public SourceStatusResponse sourceStatus(
      @ToolParam(description = "The id of the source to check") String sourceId) {
    return ToolErrors.mapped(
        () -> SourceStatusResponse.from(sourceId, syncService.freshnessOf(sourceId)));
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
