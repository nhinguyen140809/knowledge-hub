package com.knowledgehub.system.infrastructure.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the application's {@code @Tool}-annotated beans with the MCP server. The MCP server
 * auto-configuration discovers {@link ToolCallbackProvider} beans and exposes their tools to
 * agents.
 */
@Configuration
public class McpToolsConfig {

  @Bean
  public ToolCallbackProvider knowledgeHubTools(SystemTools systemTools) {
    return MethodToolCallbackProvider.builder().toolObjects(systemTools).build();
  }
}
