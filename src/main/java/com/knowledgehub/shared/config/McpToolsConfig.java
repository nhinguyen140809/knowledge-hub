package com.knowledgehub.shared.config;

import com.knowledgehub.knowledge.sync.infrastructure.mcp.SyncSourceTools;
import com.knowledgehub.retrieval.infrastructure.mcp.RetrievalTools;
import com.knowledgehub.system.infrastructure.mcp.SystemTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers every feature's {@code @Tool}-annotated bean with the MCP server. The server's
 * auto-configuration discovers {@link ToolCallbackProvider} beans and exposes their tools to
 * agents. Each tool is an inbound adapter that delegates to the same application service its REST
 * counterpart uses, so the two interfaces never drift.
 */
@Configuration
public class McpToolsConfig {

  @Bean
  public ToolCallbackProvider knowledgeHubTools(
      SystemTools systemTools, RetrievalTools retrievalTools, SyncSourceTools syncSourceTools) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(systemTools, retrievalTools, syncSourceTools)
        .build();
  }
}
