package com.knowledgehub.knowledge.indexing.infrastructure.persistence;

import com.knowledgehub.knowledge.analysis.domain.CodeEntity;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link CodeEntityRepository}. Writes {@code :CodeEntity} nodes and the structural
 * hierarchy: a top-level entity is {@code DECLARES}d by its {@code :File}; a member is {@code
 * CONTAINS}ed by its parent entity. Idempotent {@code MERGE}s keyed by {@code entity_id};
 * parameterized Cypher only.
 */
@Component
class Neo4jCodeEntityAdapter implements CodeEntityRepository {

  private static final String UPSERT =
      """
      UNWIND $rows AS row
      MERGE (f:File {file_id: row.file_id})
      MERGE (e:CodeEntity {entity_id: row.entity_id})
        SET e.source_id = row.source_id, e.file_id = row.file_id, e.level = row.level,
            e.name = row.name, e.qualified_name = row.qualified_name,
            e.signature = row.signature, e.line_start = row.line_start,
            e.line_end = row.line_end
      FOREACH (_ IN CASE WHEN row.parent_entity_id IS NULL THEN [1] ELSE [] END |
        MERGE (f)-[:DECLARES]->(e))
      FOREACH (_ IN CASE WHEN row.parent_entity_id IS NULL THEN [] ELSE [1] END |
        MERGE (p:CodeEntity {entity_id: row.parent_entity_id})
        MERGE (p)-[:CONTAINS]->(e))
      """;

  private static final String DELETE_BY_SOURCE =
      "MATCH (e:CodeEntity {source_id: $sourceId}) DETACH DELETE e";

  private final Neo4jClient client;

  Neo4jCodeEntityAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public void upsertAll(List<CodeEntity> entities) {
    if (entities.isEmpty()) {
      return;
    }
    List<Map<String, Object>> rows = entities.stream().map(Neo4jCodeEntityAdapter::toRow).toList();
    client.query(UPSERT).bind(rows).to("rows").run();
  }

  @Override
  public void deleteBySource(String sourceId) {
    client.query(DELETE_BY_SOURCE).bind(sourceId).to("sourceId").run();
  }

  private static Map<String, Object> toRow(CodeEntity entity) {
    Map<String, Object> row = new HashMap<>();
    row.put("entity_id", entity.entityId());
    row.put("source_id", entity.sourceId());
    row.put("file_id", entity.fileId());
    row.put("parent_entity_id", entity.parentEntityId());
    row.put("level", entity.level().name());
    row.put("name", entity.name());
    row.put("qualified_name", entity.qualifiedName());
    row.put("signature", entity.signature());
    row.put("line_start", entity.lineStart());
    row.put("line_end", entity.lineEnd());
    return row;
  }
}
