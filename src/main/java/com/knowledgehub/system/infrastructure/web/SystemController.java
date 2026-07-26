package com.knowledgehub.system.infrastructure.web;

import com.knowledgehub.system.application.DependencyHealthService;
import com.knowledgehub.system.application.KnowledgeStatsService;
import com.knowledgehub.system.application.SystemInfoService;
import com.knowledgehub.system.domain.DependencyStatus;
import com.knowledgehub.system.domain.KnowledgeStats;
import com.knowledgehub.system.domain.SystemInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints exposing service status and runtime information. */
@RestController
@RequestMapping("/system") // /api/v1 prefix added centrally by WebConfig
@Tag(name = "System", description = "Service status and runtime information")
public class SystemController {

  private final SystemInfoService systemInfoService;
  private final KnowledgeStatsService knowledgeStatsService;
  private final DependencyHealthService dependencyHealthService;

  public SystemController(
      SystemInfoService systemInfoService,
      KnowledgeStatsService knowledgeStatsService,
      DependencyHealthService dependencyHealthService) {
    this.systemInfoService = systemInfoService;
    this.knowledgeStatsService = knowledgeStatsService;
    this.dependencyHealthService = dependencyHealthService;
  }

  @GetMapping("/info")
  @Operation(summary = "Get service runtime information")
  public SystemInfo info() {
    return systemInfoService.currentInfo();
  }

  @GetMapping("/knowledge-stats")
  @Operation(summary = "Scale of the knowledge base: graph size and vector count")
  public KnowledgeStats knowledgeStats() {
    return knowledgeStatsService.currentStats();
  }

  @GetMapping("/dependencies")
  @Operation(summary = "Reachability of Neo4j, Qdrant and the embedding provider")
  public List<DependencyStatus> dependencies() {
    return dependencyHealthService.currentStatus();
  }

  @PutMapping("/product-name")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Set the display product name")
  public SystemInfo setProductName(@Valid @RequestBody UpdateProductNameRequest request) {
    systemInfoService.setProductName(request.productName());
    return systemInfoService.currentInfo();
  }
}
