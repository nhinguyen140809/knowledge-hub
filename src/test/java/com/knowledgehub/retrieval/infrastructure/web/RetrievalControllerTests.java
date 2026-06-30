package com.knowledgehub.retrieval.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.RankedResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RetrievalController.class)
class RetrievalControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RetrievalService retrievalService;

  @Test
  void returnsRankedHitsAsJson() throws Exception {
    Hit hit =
        new Hit(
            "chunk-1",
            0.9,
            new HitMetadata(
                "chunk", "src-a", "Greeter.java", 3, 7, "code", "main", null, null, List.of()));
    when(retrievalService.retrieve(any(), any())).thenReturn(new RankedResult(List.of(hit), false));

    mockMvc
        .perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"how does the greeter greet\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hits[0].id").value("chunk-1"))
        .andExpect(jsonPath("$.hits[0].relevanceScore").value(0.9))
        .andExpect(jsonPath("$.hits[0].metadata.path").value("Greeter.java"))
        .andExpect(jsonPath("$.hits[0].metadata.lineStart").value(3))
        .andExpect(jsonPath("$.servedFromCanonicalRef").value(false));
  }

  @Test
  void rejectsABlankQuery() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
