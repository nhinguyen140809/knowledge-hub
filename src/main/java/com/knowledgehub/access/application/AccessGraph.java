package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.AccessGraphEdgeKind;
import com.knowledgehub.access.domain.AccessGraphNodeKind;
import java.util.List;

/**
 * The subgraph that explains one principal's access, render-ready: the principal, every group
 * reachable through membership (transitively), every source those reach, and the edges between
 * them — nothing else from the org. No coordinates; layout is the frontend's job.
 *
 * @param focus the principal this graph explains
 * @param nodes every principal and source in the subgraph
 * @param edges membership (group to member) and grant (principal to source) edges among them
 */
public record AccessGraph(String focus, List<Node> nodes, List<Edge> edges) {

  public record Node(String id, AccessGraphNodeKind kind) {}

  public record Edge(String from, String to, AccessGraphEdgeKind kind) {}
}
