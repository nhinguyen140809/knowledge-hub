package com.knowledgehub.system.infrastructure.mcp;

import com.knowledgehub.system.application.SystemInfoService;
import com.knowledgehub.system.domain.SystemInfo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/** MCP tools exposing service status to AI agents. */
@Component
public class SystemTools {

  private final SystemInfoService systemInfoService;

  public SystemTools(SystemInfoService systemInfoService) {
    this.systemInfoService = systemInfoService;
  }

  @Tool(
      name = "system_info",
      description =
          "Return Knowledge Hub runtime information: application name, version, active profiles, "
              + "and the active vector store mode.")
  public SystemInfo systemInfo() {
    return systemInfoService.currentInfo();
  }
}
