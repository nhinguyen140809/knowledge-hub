package com.knowledgehub.access.infrastructure.web.principal;

import com.knowledgehub.access.application.PrincipalGraph;
import java.util.List;
import java.util.Map;

/**
 * JSON view of the full principal graph: every principal plus direct group membership edges, so the
 * tree view can draw its first frame without an N+1 walk.
 */
public record PrincipalGraphResponse(
    List<PrincipalResponse> principals, Map<String, List<String>> membership) {

  static PrincipalGraphResponse from(PrincipalGraph graph) {
    return new PrincipalGraphResponse(
        graph.principals().stream().map(PrincipalResponse::from).toList(), graph.membership());
  }
}
