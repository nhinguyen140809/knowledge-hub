package com.knowledgehub.knowledge.graph.infrastructure.persistence;

import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.RelationshipRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link RelationshipRepository}. The source end of a relationship is matched by
 * either its {@code entity_id} (structural, code-to-code) or {@code chunk_id} (cross-artifact, a
 * document chunk pointing at code); the target end is always a {@code :CodeEntity}. Writes are
 * idempotent {@code MERGE}s keyed by {@code (from, to, type)}.
 *
 * <p>The relationship type cannot be a Cypher parameter, so relationships are grouped by type and
 * the type name is interpolated into the query. This is safe because {@link RelationType} is a
 * closed enum, never user input; every other value stays parameterized.
 */
@Component
class Neo4jRelationshipAdapter implements RelationshipRepository {

  /**
   * Relationship types this adapter owns and may delete on re-link. Excludes {@code CONTAINS} and
   * {@code DECLARES}, which the indexing entity hierarchy owns.
   */
  private static final List<String> MANAGED_TYPES =
      Arrays.stream(RelationType.values())
          .filter(t -> t != RelationType.CONTAINS && t != RelationType.DECLARES)
          .map(Enum::name)
          .toList();

  private static final String DELETE_BY_SOURCE =
      "MATCH (from)-[r]->() WHERE type(r) IN $types AND from.source_id = $sourceId DELETE r";

  private final Neo4jClient client;

  Neo4jRelationshipAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public void upsertAll(List<Relationship> relationships) {
    if (relationships.isEmpty()) {
      return;
    }
    Map<RelationType, List<Relationship>> byType =
        relationships.stream().collect(Collectors.groupingBy(Relationship::type));
    byType.forEach(
        (type, rels) -> {
          List<Map<String, Object>> rows =
              rels.stream().map(Neo4jRelationshipAdapter::toRow).toList();
          client.query(upsert(type)).bind(rows).to("rows").run();
        });
  }

  @Override
  public void deleteBySource(String sourceId) {
    client
        .query(DELETE_BY_SOURCE)
        .bind(MANAGED_TYPES)
        .to("types")
        .bind(sourceId)
        .to("sourceId")
        .run();
  }

  /**
   * Builds the type-specific MERGE; {@code type.name()} is a closed enum value, so safe to inline.
   */
  private static String upsert(RelationType type) {
    return """
        UNWIND $rows AS row
        OPTIONAL MATCH (e:CodeEntity {entity_id: row.from_id})
        OPTIONAL MATCH (c:Chunk {chunk_id: row.from_id})
        WITH row, coalesce(e, c) AS from
        WHERE from IS NOT NULL
        MATCH (to:CodeEntity {entity_id: row.to_id})
        MERGE (from)-[r:`%s`]->(to)
          SET r.confidence = row.confidence, r.evidence = row.evidence
        """
        .formatted(type.name());
  }

  private static Map<String, Object> toRow(Relationship rel) {
    Map<String, Object> row = new HashMap<>();
    row.put("from_id", rel.fromId());
    row.put("to_id", rel.toId());
    row.put("confidence", rel.confidence());
    row.put("evidence", rel.evidence());
    return row;
  }
}
