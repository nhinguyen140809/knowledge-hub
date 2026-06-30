package com.knowledgehub.retrieval.infrastructure.web;

import com.knowledgehub.retrieval.application.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Query API: free text in, a ranked JSON result out. The {@code /api/v1} prefix is added by
 * WebConfig. Access control is not yet wired in, so every query runs unrestricted (the {@code
 * allowedSources} pre-filter is threaded through as "all sources").
 */
@RestController
@RequestMapping("/query")
@Tag(name = "Retrieval", description = "Hybrid search over the knowledge graph")
public class RetrievalController {

  private final RetrievalService retrievalService;

  public RetrievalController(RetrievalService retrievalService) {
    this.retrievalService = retrievalService;
  }

  @PostMapping
  @Operation(summary = "Run a hybrid (semantic + keyword + graph) query")
  public RankedResultResponse query(@Valid @RequestBody QueryRequest request) {
    return RankedResultResponse.from(retrievalService.retrieve(request.toQuery(), null));
  }
}
