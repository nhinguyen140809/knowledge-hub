package com.knowledgehub.knowledge.infrastructure.persistence;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Creates the Neo4j schema on startup: uniqueness constraints, full-text (BM25) indexes, property
 * indexes for content-hash dedup, and the vector index on {@code :Chunk(embedding)}. All statements
 * are idempotent ({@code IF NOT EXISTS}); there is no separate migration tool — schema setup is
 * just idempotent Cypher (see {@code docs/plan/00-foundation-and-schema.md} §3.3).
 */
@Component
public class SchemaInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

  private static final List<String> CONSTRAINTS_AND_INDEXES =
      List.of(
          // uniqueness — every node has a unique identity key
          "CREATE CONSTRAINT source_id IF NOT EXISTS"
              + " FOR (s:Source) REQUIRE s.source_id IS UNIQUE",
          "CREATE CONSTRAINT file_id IF NOT EXISTS FOR (f:File) REQUIRE f.file_id IS UNIQUE",
          "CREATE CONSTRAINT chunk_id IF NOT EXISTS FOR (c:Chunk) REQUIRE c.chunk_id IS UNIQUE",
          "CREATE CONSTRAINT entity_id IF NOT EXISTS"
              + " FOR (e:CodeEntity) REQUIRE e.entity_id IS UNIQUE",
          "CREATE CONSTRAINT principal_id IF NOT EXISTS"
              + " FOR (p:Principal) REQUIRE p.principal_id IS UNIQUE",
          // O(1) credential lookup per request during authentication
          "CREATE CONSTRAINT cred_hash IF NOT EXISTS"
              + " FOR (c:Credential) REQUIRE c.hash IS UNIQUE",
          // full-text (BM25 keyword search, FR-4.2)
          "CREATE FULLTEXT INDEX chunk_text IF NOT EXISTS FOR (c:Chunk) ON EACH [c.text]",
          "CREATE FULLTEXT INDEX entity_name IF NOT EXISTS"
              + " FOR (e:CodeEntity) ON EACH [e.name, e.signature]",
          // fast dedup by content hash (FR-6.3)
          "CREATE INDEX chunk_hash IF NOT EXISTS FOR (c:Chunk) ON (c.content_hash)",
          "CREATE INDEX file_hash IF NOT EXISTS FOR (f:File) ON (f.content_hash)");

  private final Neo4jClient neo4jClient;
  private final int embeddingDimension;

  public SchemaInitializer(
      Neo4jClient neo4jClient,
      @Value("${spring.ai.vectorstore.neo4j.embedding-dimension:1536}") int embeddingDimension) {
    this.neo4jClient = neo4jClient;
    this.embeddingDimension = embeddingDimension;
  }

  @Override
  public void run(ApplicationArguments args) {
    for (String statement : CONSTRAINTS_AND_INDEXES) {
      neo4jClient.query(statement).run();
    }
    neo4jClient.query(vectorIndexStatement()).run();
    log.info(
        "Neo4j schema initialized: {} constraints/indexes + chunk vector index (dim {})",
        CONSTRAINTS_AND_INDEXES.size(),
        embeddingDimension);
  }

  /**
   * Vector index on {@code :Chunk(embedding)}. The dimension is inlined (not a query parameter)
   * because Neo4j index OPTIONS do not accept parameters; the value is a trusted int from config.
   */
  private String vectorIndexStatement() {
    return "CREATE VECTOR INDEX chunk_embedding IF NOT EXISTS"
        + " FOR (c:Chunk) ON (c.embedding)"
        + " OPTIONS { indexConfig: {"
        + " `vector.dimensions`: "
        + embeddingDimension
        + ", `vector.similarity_function`: 'cosine' } }";
  }
}
