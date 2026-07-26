package com.knowledgehub.system.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.knowledgehub.system.application.KnowledgeStatsService;
import com.knowledgehub.system.application.SystemInfoService;
import com.knowledgehub.system.domain.KnowledgeStats;
import com.knowledgehub.system.domain.SystemInfo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SystemInfoService systemInfoService;
  @MockitoBean private KnowledgeStatsService knowledgeStatsService;

  @Test
  void returnsSystemInfoAsJson() throws Exception {
    when(systemInfoService.currentInfo())
        .thenReturn(new SystemInfo("Knowledge Hub", "knowledge-hub", "0.0.1", List.of("dev")));

    mockMvc
        .perform(get("/api/v1/system/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productName").value("Knowledge Hub"))
        .andExpect(jsonPath("$.application").value("knowledge-hub"))
        .andExpect(jsonPath("$.version").value("0.0.1"))
        .andExpect(jsonPath("$.activeProfiles[0]").value("dev"));
  }

  @Test
  void setsProductNameAndReturnsUpdatedInfo() throws Exception {
    when(systemInfoService.currentInfo())
        .thenReturn(new SystemInfo("Renamed", "knowledge-hub", "0.0.1", List.of("dev")));

    mockMvc
        .perform(
            put("/api/v1/system/product-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productName\":\"Renamed\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productName").value("Renamed"));

    verify(systemInfoService).setProductName("Renamed");
  }

  @Test
  void rejectsBlankProductNameWith400() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/system/product-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productName\":\"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void returnsKnowledgeStatsAsJson() throws Exception {
    when(knowledgeStatsService.currentStats()).thenReturn(new KnowledgeStats(1240, 8300, 19400, 1240));

    mockMvc
        .perform(get("/api/v1/system/knowledge-stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documents").value(1240))
        .andExpect(jsonPath("$.graphNodes").value(8300))
        .andExpect(jsonPath("$.graphEdges").value(19400))
        .andExpect(jsonPath("$.vectors").value(1240));
  }
}
