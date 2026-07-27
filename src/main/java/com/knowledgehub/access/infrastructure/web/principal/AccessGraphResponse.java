package com.knowledgehub.access.infrastructure.web.principal;

import com.knowledgehub.access.application.AccessGraph;
import com.knowledgehub.access.domain.AccessGraphEdgeKind;
import com.knowledgehub.access.domain.AccessGraphNodeKind;
import java.util.List;

/** JSON view of one principal's access subgraph: nodes and edges only, no coordinates. */
public record AccessGraphResponse(
    String focus, List<NodeResponse> nodes, List<EdgeResponse> edges) {

  static AccessGraphResponse from(AccessGraph graph) {
    return new AccessGraphResponse(
        graph.focus(),
        graph.nodes().stream().map(NodeResponse::from).toList(),
        graph.edges().stream().map(EdgeResponse::from).toList());
  }

  public record NodeResponse(String id, AccessGraphNodeKind kind) {
    static NodeResponse from(AccessGraph.Node node) {
      return new NodeResponse(node.id(), node.kind());
    }
  }

  public record EdgeResponse(String from, String to, AccessGraphEdgeKind kind) {
    static EdgeResponse from(AccessGraph.Edge edge) {
      return new EdgeResponse(edge.from(), edge.to(), edge.kind());
    }
  }
}
