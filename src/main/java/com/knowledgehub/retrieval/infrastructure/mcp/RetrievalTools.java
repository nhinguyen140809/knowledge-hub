package com.knowledgehub.retrieval.infrastructure.mcp;

import com.knowledgehub.access.infrastructure.security.AclFilterProvider;
import com.knowledgehub.retrieval.application.RetrievalService;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.QueryParams;
import com.knowledgehub.retrieval.domain.RankedResult;
import com.knowledgehub.shared.error.ToolErrors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tool exposing hybrid retrieval to AI agents. It is an inbound adapter over the same {@link
 * RetrievalService} the REST controller calls — no query logic lives here. Results are scoped to
 * the authenticated caller's readable sources, supplied by {@link AclFilterProvider}, so an agent
 * never sees a source it may not read.
 */
@Component
public class RetrievalTools {

  private final RetrievalService retrievalService;
  private final AclFilterProvider aclFilterProvider;

  public RetrievalTools(RetrievalService retrievalService, AclFilterProvider aclFilterProvider) {
    this.retrievalService = retrievalService;
    this.aclFilterProvider = aclFilterProvider;
  }

  @Tool(
      name = "query_knowledge",
      description =
          "Search the knowledge hub for relevant code, docs, requirements, and commits. Runs a "
              + "hybrid semantic + keyword + graph query and returns ranked hits, best first. Each "
              + "hit carries a relevance score and metadata (source, path, line range, ref/commit) "
              + "so you can cite it. Results are limited to the sources you may read.")
  public RankedResult queryKnowledge(
      @ToolParam(description = "The free-text query") String text,
      @ToolParam(required = false, description = "Maximum number of results") Integer topK,
      @ToolParam(
              required = false,
              description =
                  "Restrict to a single source id; narrows within the sources you may read")
          String sourceId,
      @ToolParam(required = false, description = "Restrict to a version/branch ref") String ref,
      @ToolParam(
              required = false,
              description = "Restrict to a data type: code, doc, requirement, or commit")
          String type) {
    return ToolErrors.mapped(
        () ->
            retrievalService.retrieve(
                new Query(text, new QueryParams(topK, sourceId, ref, type)),
                aclFilterProvider.currentAllowedSources()));
  }
}
