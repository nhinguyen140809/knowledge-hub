package com.knowledgehub.retrieval.infrastructure.search;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.GraphTraversalPort;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * One-hop graph expansion from the seed hits: every chunk/entity/commit adjacent to a seed becomes
 * a candidate, scored by how many seeds reach it (a node related to several seeds is more central
 * to the query). This is the GraphRAG step that pulls in context - a caller, a containing type, a
 * describing doc, a commit that touched the file - that a pure vector or keyword match would miss.
 *
 * <p>Commits attach to the graph through {@code :File} nodes ({@code MODIFIES} edges), and files
 * are not results themselves, so the traversal adds one deliberate bridge: seed → its file → the
 * commits that modified it, and the reverse for a commit seed. Nothing else crosses two hops.
 *
 * <p>{@code allowedSources} is pushed into the traversal as a hard pre-filter, so expansion never
 * crosses into a disallowed source even though it follows relationships outward.
 */
@Component
class Neo4jGraphTraverser implements GraphTraversalPort {

  private static final String EXPAND =
      """
      UNWIND $seeds AS seed
      OPTIONAL MATCH (sc:Chunk {chunk_id: seed})
      OPTIONAL MATCH (se:CodeEntity {entity_id: seed})
      OPTIONAL MATCH (sm:Commit {commit_id: seed})
      WITH $seeds AS seeds, coalesce(sc, se, sm) AS s
      WHERE s IS NOT NULL
      CALL {
        WITH s
        MATCH (s)-[]-(n)
        WHERE n:Chunk OR n:CodeEntity OR n:Commit
        RETURN n
        UNION
        WITH s
        MATCH (s)-[:PART_OF|DECLARES]-(:File)<-[:MODIFIES]-(n:Commit)
        RETURN n
        UNION
        WITH s
        MATCH (s)-[:MODIFIES]->(:File)-[:PART_OF|DECLARES]-(n)
        WHERE n:Chunk OR n:CodeEntity
        RETURN n
      }
      WITH seeds, n
      WHERE $unrestricted OR n.source_id IN $allowedSources
      WITH seeds, coalesce(n.chunk_id, n.entity_id, n.commit_id) AS id
      WHERE id IS NOT NULL AND NOT id IN seeds
      RETURN id, count(*) AS score ORDER BY score DESC LIMIT $k
      """;

  private final Neo4jClient client;

  Neo4jGraphTraverser(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public List<ScoredId> expand(Collection<String> seedIds, int k, Filter filter) {
    if (seedIds.isEmpty() || SourceFilters.restrictedToNothing(filter)) {
      return List.of();
    }
    return client
        .query(EXPAND)
        .bindAll(
            Map.of(
                "seeds", List.copyOf(seedIds),
                "unrestricted", filter.isUnrestricted(),
                "allowedSources", SourceFilters.allowedSources(filter),
                "k", k))
        .fetchAs(ScoredId.class)
        .mappedBy(
            (t, row) -> new ScoredId(row.get("id").asString(), (double) row.get("score").asLong()))
        .all()
        .stream()
        .toList();
  }
}
