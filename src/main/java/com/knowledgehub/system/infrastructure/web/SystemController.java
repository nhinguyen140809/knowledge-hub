package com.knowledgehub.system.infrastructure.web;

import com.knowledgehub.system.application.SystemInfoService;
import com.knowledgehub.system.domain.SystemInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints exposing service status and runtime information. */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Service status and runtime information")
public class SystemController {

  private final SystemInfoService systemInfoService;

  public SystemController(SystemInfoService systemInfoService) {
    this.systemInfoService = systemInfoService;
  }

  @GetMapping("/info")
  @Operation(summary = "Get service runtime information")
  public SystemInfo info() {
    return systemInfoService.currentInfo();
  }
}
