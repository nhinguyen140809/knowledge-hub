package com.knowledgehub.retrieval.infrastructure.web;

import com.knowledgehub.access.infrastructure.security.AclFilterProvider;
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
 * WebConfig. Every query is scoped to the sources the authenticated caller may read, supplied by
 * {@link AclFilterProvider} and applied as a hard pre-filter on every search path.
 */
@RestController
@RequestMapping("/query")
@Tag(name = "Retrieval", description = "Hybrid search over the knowledge graph")
public class RetrievalController {

  private final RetrievalService retrievalService;
  private final AclFilterProvider aclFilterProvider;

  public RetrievalController(
      RetrievalService retrievalService, AclFilterProvider aclFilterProvider) {
    this.retrievalService = retrievalService;
    this.aclFilterProvider = aclFilterProvider;
  }

  @PostMapping
  @Operation(summary = "Run a hybrid (semantic + keyword + graph) query")
  public RankedResultResponse query(@Valid @RequestBody QueryRequest request) {
    return RankedResultResponse.from(
        retrievalService.retrieve(request.toQuery(), aclFilterProvider.currentAllowedSources()));
  }
}
