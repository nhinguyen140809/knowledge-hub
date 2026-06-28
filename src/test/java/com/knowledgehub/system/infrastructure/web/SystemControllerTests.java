package com.knowledgehub.system.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.knowledgehub.system.application.SystemInfoService;
import com.knowledgehub.system.domain.SystemInfo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemController.class)
class SystemControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SystemInfoService systemInfoService;

  @Test
  void returnsSystemInfoAsJson() throws Exception {
    when(systemInfoService.currentInfo())
        .thenReturn(new SystemInfo("knowledge-hub", "0.0.1", List.of("dev"), "neo4j+qdrant"));

    mockMvc
        .perform(get("/api/v1/system/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application").value("knowledge-hub"))
        .andExpect(jsonPath("$.version").value("0.0.1"))
        .andExpect(jsonPath("$.activeProfiles[0]").value("dev"))
        .andExpect(jsonPath("$.vectorStore").value("neo4j+qdrant"));
  }
}
