package com.knowledgehub.knowledge.sync.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.knowledgehub.knowledge.sync.application.SyncService;
import com.knowledgehub.knowledge.sync.domain.FreshnessInfo;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SyncController.class)
class SyncControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SyncService syncService;

  @Test
  void triggersSyncAndReturnsTheResult() throws Exception {
    when(syncService.sync("s1"))
        .thenReturn(new SyncResult("s1", 2, 1, 3, 0, 123L, "abc123", false));

    mockMvc
        .perform(post("/api/v1/admin/sources/s1/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indexed").value(2))
        .andExpect(jsonPath("$.reindexed").value(1))
        .andExpect(jsonPath("$.evicted").value(3))
        .andExpect(jsonPath("$.toCommit").value("abc123"))
        .andExpect(jsonPath("$.idempotent").value(false));
  }

  @Test
  void reportsFreshnessForAnIndexedSource() throws Exception {
    when(syncService.freshnessOf("s1"))
        .thenReturn(
            Optional.of(
                new FreshnessInfo("s1", Instant.parse("2026-01-01T00:00:00Z"), "abc123", "main")));

    mockMvc
        .perform(get("/api/v1/admin/sources/s1/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indexed").value(true))
        .andExpect(jsonPath("$.commitSha").value("abc123"))
        .andExpect(jsonPath("$.ref").value("main"));
  }

  @Test
  void reportsANeverSyncedSource() throws Exception {
    when(syncService.freshnessOf("s1")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/admin/sources/s1/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indexed").value(false));
  }
}
