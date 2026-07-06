package com.knowledgehub.knowledge.infrastructure.persistence;

import com.knowledgehub.shared.config.AppProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import io.qdrant.client.grpc.Collections.VectorParams;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Creates the storage schema on startup. Neo4j holds the graph + keyword index: uniqueness
 * constraints, full-text (BM25) indexes, and property indexes for content-hash dedup. Vectors live
 * in Qdrant (always), so this also ensures the Qdrant collection exists with the right dimension
 * and cosine distance. All steps are idempotent ({@code IF NOT EXISTS} on Neo4j;
 * create-collection-if-absent on Qdrant) — there is no separate migration tool.
 */
@Component
@Order(0)
public class SchemaInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

  private static final String SOURCE_ID = "source_id";

  private static final List<String> CONSTRAINTS_AND_INDEXES =
      List.of(
          // uniqueness — every node has a unique identity key
          "CREATE CONSTRAINT source_id IF NOT EXISTS"
              + " FOR (s:Source) REQUIRE s.source_id IS UNIQUE",
          "CREATE CONSTRAINT file_id IF NOT EXISTS FOR (f:File) REQUIRE f.file_id IS UNIQUE",
          "CREATE CONSTRAINT chunk_id IF NOT EXISTS FOR (c:Chunk) REQUIRE c.chunk_id IS UNIQUE",
          "CREATE CONSTRAINT entity_id IF NOT EXISTS"
              + " FOR (e:CodeEntity) REQUIRE e.entity_id IS UNIQUE",
          "CREATE CONSTRAINT document_id IF NOT EXISTS"
              + " FOR (d:Document) REQUIRE d.document_id IS UNIQUE",
          "CREATE CONSTRAINT requirement_id IF NOT EXISTS"
              + " FOR (r:Requirement) REQUIRE r.requirement_id IS UNIQUE",
          "CREATE CONSTRAINT commit_id IF NOT EXISTS"
              + " FOR (c:Commit) REQUIRE c.commit_id IS UNIQUE",
          "CREATE CONSTRAINT principal_id IF NOT EXISTS"
              + " FOR (p:Principal) REQUIRE p.principal_id IS UNIQUE",
          "CREATE CONSTRAINT credential_id IF NOT EXISTS"
              + " FOR (c:Credential) REQUIRE c.credential_id IS UNIQUE",
          "CREATE CONSTRAINT system_config_key IF NOT EXISTS"
              + " FOR (s:SystemConfig) REQUIRE s.key IS UNIQUE",
          "CREATE CONSTRAINT source_freshness_id IF NOT EXISTS"
              + " FOR (s:SourceFreshness) REQUIRE s.source_id IS UNIQUE",
          // O(1) credential lookup per request during authentication
          "CREATE CONSTRAINT cred_hash IF NOT EXISTS"
              + " FOR (c:Credential) REQUIRE c.hash IS UNIQUE",
          // full-text (BM25 keyword search)
          "CREATE FULLTEXT INDEX chunk_text IF NOT EXISTS FOR (c:Chunk) ON EACH [c.text]",
          "CREATE FULLTEXT INDEX entity_name IF NOT EXISTS"
              + " FOR (e:CodeEntity) ON EACH [e.name, e.signature]",
          "CREATE FULLTEXT INDEX commit_message IF NOT EXISTS"
              + " FOR (c:Commit) ON EACH [c.message]",
          // fast dedup by content hash
          "CREATE INDEX chunk_hash IF NOT EXISTS FOR (c:Chunk) ON (c.content_hash)",
          "CREATE INDEX file_hash IF NOT EXISTS FOR (f:File) ON (f.content_hash)",
          // entity resolution lookups during knowledge linking
          "CREATE INDEX entity_qualified_name IF NOT EXISTS"
              + " FOR (e:CodeEntity) ON (e.qualified_name)",
          "CREATE INDEX entity_name_lookup IF NOT EXISTS FOR (e:CodeEntity) ON (e.name)",
          "CREATE INDEX entity_source IF NOT EXISTS FOR (e:CodeEntity) ON (e.source_id)",
          "CREATE INDEX file_path IF NOT EXISTS FOR (f:File) ON (f.path)",
          // commit dedup during history indexing
          "CREATE INDEX commit_source_sha IF NOT EXISTS FOR (c:Commit) ON (c.source_id, c.sha)");

  private final Neo4jClient neo4jClient;
  private final QdrantClient qdrantClient;
  private final String collectionName;
  private final int embeddingDimension;

  public SchemaInitializer(
      Neo4jClient neo4jClient,
      QdrantClient qdrantClient,
      @Value("${spring.ai.vectorstore.qdrant.collection-name:knowledge-embeddings}")
          String collectionName,
      AppProperties properties) {
    this.neo4jClient = neo4jClient;
    this.qdrantClient = qdrantClient;
    this.collectionName = collectionName;
    this.embeddingDimension = properties.embedding().dimension();
  }

  @Override
  public void run(ApplicationArguments args) {
    for (String statement : CONSTRAINTS_AND_INDEXES) {
      neo4jClient.query(statement).run();
    }
    ensureQdrantCollection();
    log.info(
        "Schema initialized: {} Neo4j constraints/indexes + Qdrant collection '{}' (dim {}, cosine)",
        CONSTRAINTS_AND_INDEXES.size(),
        collectionName,
        embeddingDimension);
  }

  /**
   * Creates the Qdrant collection (cosine, configured dimension) if it does not exist yet, and
   * ensures a payload index on {@code source_id} — the field every ACL-filtered search matches on.
   * Without it, a highly selective filter (a principal granted only a small source) makes Qdrant's
   * HNSW search traverse far more of the graph to satisfy top-k, inflating tail latency. Both calls
   * are idempotent, so this also backfills the index on a collection created before this existed.
   */
  private void ensureQdrantCollection() {
    try {
      if (!Boolean.TRUE.equals(qdrantClient.collectionExistsAsync(collectionName).get())) {
        qdrantClient
            .createCollectionAsync(
                collectionName,
                VectorParams.newBuilder()
                    .setSize(embeddingDimension)
                    .setDistance(Distance.Cosine)
                    .build())
            .get();
      }
      qdrantClient
          .createPayloadIndexAsync(
              collectionName, SOURCE_ID, PayloadSchemaType.Keyword, null, null, null, null)
          .get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while ensuring Qdrant collection", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to ensure Qdrant collection " + collectionName, e);
    }
  }
}
