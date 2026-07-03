package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.knowledgehub.knowledge.ingestion.application.DuplicateSourceException;
import com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException;
import com.knowledgehub.knowledge.ingestion.application.SourceService;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SourceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SourceControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SourceService sourceService;

  private static Source gitSource() {
    return new Source(
        "s1", SourceType.GIT, "https://x/y.git", "main", List.of("**/*.java"), List.of("target"));
  }

  @Test
  void createReturns201WithLocationAndBody() throws Exception {
    when(sourceService.register(any())).thenReturn(gitSource());

    mockMvc
        .perform(
            post("/api/v1/admin/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"s1","type":"GIT","uriOrPath":"https://x/y.git","ref":"main",
                     "include":["**/*.java"],"ignore":["target"]}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/v1/admin/sources/s1"))
        .andExpect(jsonPath("$.id").value("s1"))
        .andExpect(jsonPath("$.type").value("GIT"))
        .andExpect(jsonPath("$.ref").value("main"));
  }

  @Test
  void createRejectsInvalidBodyWith400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"s1\",\"type\":\"GIT\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void getReturns404WhenMissing() throws Exception {
    when(sourceService.get("nope")).thenThrow(new SourceNotFoundException("nope"));

    mockMvc
        .perform(get("/api/v1/admin/sources/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SOURCE_NOT_FOUND"));
  }

  @Test
  void createReturns409OnDuplicate() throws Exception {
    when(sourceService.register(any())).thenThrow(new DuplicateSourceException("s1"));

    mockMvc
        .perform(
            post("/api/v1/admin/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"s1\",\"type\":\"FS\",\"uriOrPath\":\"/data\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_SOURCE"));
  }

  @Test
  void updateReturns200WithUpdatedBody() throws Exception {
    when(sourceService.update(any(), any(), any(), any())).thenReturn(gitSource());

    mockMvc
        .perform(
            put("/api/v1/admin/sources/s1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref\":\"main\",\"include\":[\"**/*.java\"],\"ignore\":[\"target\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("s1"))
        .andExpect(jsonPath("$.include[0]").value("**/*.java"));
  }

  @Test
  void updateReturns404WhenMissing() throws Exception {
    when(sourceService.update(any(), any(), any(), any()))
        .thenThrow(new SourceNotFoundException("nope"));

    mockMvc
        .perform(
            put("/api/v1/admin/sources/nope").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SOURCE_NOT_FOUND"));
  }

  @Test
  void listReturnsSources() throws Exception {
    when(sourceService.list()).thenReturn(List.of(gitSource()));

    mockMvc
        .perform(get("/api/v1/admin/sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("s1"));
  }

  @Test
  void deleteReturns204() throws Exception {
    mockMvc.perform(delete("/api/v1/admin/sources/s1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteReturns404WhenMissing() throws Exception {
    doThrow(new SourceNotFoundException("nope")).when(sourceService).remove("nope");

    mockMvc
        .perform(delete("/api/v1/admin/sources/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SOURCE_NOT_FOUND"));
  }
}
