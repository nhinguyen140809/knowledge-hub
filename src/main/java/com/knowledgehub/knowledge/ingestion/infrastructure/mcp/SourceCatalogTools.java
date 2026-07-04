package com.knowledgehub.knowledge.ingestion.infrastructure.mcp;

import com.knowledgehub.access.infrastructure.security.AclFilterProvider;
import com.knowledgehub.knowledge.ingestion.application.SourceService;
import com.knowledgehub.shared.error.ToolErrors;
import java.util.List;
import java.util.Set;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * MCP tool letting an agent discover which knowledge sources it may read. It lists the configured
 * sources intersected with the caller's readable set — the same set every query is scoped to — so
 * it reveals nothing a query could not already surface. This is the entry point of the agent loop:
 * discover sources here, check freshness with {@code source_status}, sync if stale, then query.
 */
@Component
public class SourceCatalogTools {

  private final SourceService sourceService;
  private final AclFilterProvider aclFilterProvider;

  public SourceCatalogTools(SourceService sourceService, AclFilterProvider aclFilterProvider) {
    this.sourceService = sourceService;
    this.aclFilterProvider = aclFilterProvider;
  }

  @Tool(
      name = "list_sources",
      description =
          "List the knowledge sources you are allowed to read, each with its id, kind (git or "
              + "filesystem) and configured ref. Call this first to discover what you can query; "
              + "then use source_status to check how fresh a source's index is. Read-only and "
              + "available to every authenticated agent.")
  public List<ReadableSourceResponse> listSources() {
    return ToolErrors.mapped(
        () -> {
          Set<String> allowed = aclFilterProvider.currentAllowedSources();
          return sourceService.list().stream()
              .filter(source -> allowed.contains(source.sourceId()))
              .map(ReadableSourceResponse::from)
              .toList();
        });
  }
}
