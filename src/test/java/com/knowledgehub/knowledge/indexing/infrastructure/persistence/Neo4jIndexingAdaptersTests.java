package com.knowledgehub.knowledge.indexing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.indexing.domain.CodeEntity;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityLevel;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.Provenance;
import com.knowledgehub.shared.id.IdFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Neo4jIndexingAdaptersTests {

  private static final String SOURCE = "it-indexing";
  private static final String PATH = "Greeter.java";

  @Autowired private ChunkRepository chunks;
  @Autowired private CodeEntityRepository entities;
  @Autowired private Neo4jClient neo4j;

  @AfterEach
  void cleanUp() {
    chunks.deleteBySource(SOURCE);
    entities.deleteBySource(SOURCE);
  }

  private static Provenance provenance() {
    return new FsProvenance(SOURCE, PATH, "file-hash", Instant.EPOCH);
  }

  private static String fileId() {
    return IdFactory.fileId(SOURCE, PATH);
  }

  private static Chunk codeChunk(String text, String entityId) {
    String hash = "h-" + text.hashCode();
    return new Chunk(
        IdFactory.chunkId(SOURCE, PATH, hash),
        SOURCE,
        fileId(),
        PATH,
        ChunkType.CODE,
        text,
        hash,
        3,
        1,
        5,
        entityId,
        provenance());
  }

  private long count(String cypher) {
    return neo4j.query(cypher).fetchAs(Long.class).one().orElse(0L);
  }

  @Test
  void upsertsChunksAndEntitiesWithStructuralRelationships() {
    String typeId = IdFactory.entityId(SOURCE, PATH, "Greeter");
    String methodId = IdFactory.entityId(SOURCE, PATH, "Greeter#greet");
    entities.upsertAll(
        List.of(
            new CodeEntity(
                typeId,
                SOURCE,
                fileId(),
                null,
                CodeEntityLevel.CLASS,
                "Greeter",
                "Greeter",
                "class Greeter",
                1,
                20),
            new CodeEntity(
                methodId,
                SOURCE,
                fileId(),
                typeId,
                CodeEntityLevel.METHOD,
                "greet",
                "Greeter#greet",
                "String greet()",
                10,
                14)));
    chunks.upsertAll(List.of(codeChunk("String greet() { return name; }", methodId)));

    assertThat(count("MATCH (c:Chunk {source_id: '" + SOURCE + "'}) RETURN count(c)")).isEqualTo(1);
    assertThat(
            count(
                "MATCH (:Chunk {source_id: '" + SOURCE + "'})-[:PART_OF]->(:File) RETURN count(*)"))
        .isEqualTo(1);
    assertThat(
            count(
                "MATCH (:Chunk {source_id: '"
                    + SOURCE
                    + "'})-[:CHUNK_OF]->(:CodeEntity) RETURN count(*)"))
        .isEqualTo(1);
    assertThat(
            count(
                "MATCH (:File {file_id: '"
                    + fileId()
                    + "'})-[:DECLARES]->(:CodeEntity) RETURN count(*)"))
        .isEqualTo(1);
    assertThat(
            count(
                "MATCH (:CodeEntity {entity_id: '"
                    + typeId
                    + "'})-[:CONTAINS]->(:CodeEntity) RETURN count(*)"))
        .isEqualTo(1);
  }

  @Test
  void reUpsertIsIdempotent() {
    Chunk chunk = codeChunk("body", null);
    chunks.upsertAll(List.of(chunk));
    chunks.upsertAll(List.of(chunk));

    assertThat(count("MATCH (c:Chunk {source_id: '" + SOURCE + "'}) RETURN count(c)")).isEqualTo(1);
  }

  @Test
  void existingContentHashesReturnsOnlyIndexedOnes() {
    Chunk chunk = codeChunk("indexed body", null);
    chunks.upsertAll(List.of(chunk));

    Set<String> present =
        chunks.existingContentHashes(SOURCE, List.of(chunk.contentHash(), "absent-hash"));

    assertThat(present).containsExactly(chunk.contentHash());
  }

  @Test
  void deleteBySourceRemovesChunkNodes() {
    chunks.upsertAll(List.of(codeChunk("to delete", null)));
    chunks.deleteBySource(SOURCE);

    assertThat(count("MATCH (c:Chunk {source_id: '" + SOURCE + "'}) RETURN count(c)")).isZero();
  }

  @Test
  void chunkCarriesProvenanceAndMetadata() {
    chunks.upsertAll(List.of(codeChunk("traced body", null)));

    Map<String, Object> props =
        neo4j
            .query("MATCH (c:Chunk {source_id: '" + SOURCE + "'}) RETURN c{.*} AS c LIMIT 1")
            .fetchAs(java.util.Map.class)
            .one()
            .map(m -> (Map<String, Object>) m)
            .orElseThrow();
    assertThat(props).containsEntry("type", "code").containsEntry("path", PATH);
    assertThat(props.get("indexed_at")).isEqualTo(Instant.EPOCH.toString());
  }
}
