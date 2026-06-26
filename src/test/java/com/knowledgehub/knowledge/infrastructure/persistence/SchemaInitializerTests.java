package com.knowledgehub.knowledge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaInitializerTests {

  @Autowired private Neo4jClient neo4jClient;

  @Test
  void createsUniquenessConstraints() {
    List<String> names = names("SHOW CONSTRAINTS YIELD name RETURN name");

    assertThat(names)
        .contains("source_id", "file_id", "chunk_id", "entity_id", "principal_id", "cred_hash");
  }

  @Test
  void createsFullTextPropertyAndVectorIndexes() {
    List<String> names = names("SHOW INDEXES YIELD name RETURN name");

    assertThat(names)
        .contains("chunk_text", "entity_name", "chunk_hash", "file_hash", "chunk_embedding");
  }

  private List<String> names(String cypher) {
    return neo4jClient
        .query(cypher)
        .fetchAs(String.class)
        .mappedBy((t, r) -> r.get("name").asString())
        .all()
        .stream()
        .toList();
  }
}
