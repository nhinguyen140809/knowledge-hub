package com.knowledgehub.knowledge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import io.qdrant.client.QdrantClient;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaInitializerTests {

  @Autowired private Neo4jClient neo4jClient;
  @Autowired private QdrantClient qdrantClient;

  @Value("${spring.ai.vectorstore.qdrant.collection-name:knowledge-embeddings}")
  private String collectionName;

  @Test
  void createsUniquenessConstraints() {
    List<String> names = names("SHOW CONSTRAINTS YIELD name RETURN name");

    assertThat(names)
        .contains(
            "source_id",
            "file_id",
            "chunk_id",
            "entity_id",
            "document_id",
            "requirement_id",
            "commit_sha",
            "principal_id",
            "credential_id",
            "system_config_key",
            "source_freshness_id",
            "cred_hash");
  }

  @Test
  void createsFullTextAndPropertyIndexes() {
    List<String> names = names("SHOW INDEXES YIELD name RETURN name");

    assertThat(names)
        .contains(
            "chunk_text",
            "entity_name",
            "chunk_hash",
            "file_hash",
            "entity_qualified_name",
            "entity_name_lookup",
            "entity_source",
            "file_path");
  }

  @Test
  void createsTheQdrantCollection() throws ExecutionException, InterruptedException {
    assertThat(qdrantClient.collectionExistsAsync(collectionName).get()).isTrue();
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
