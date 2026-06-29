package com.knowledgehub.knowledge.graph.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.RelationshipRepository;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.indexing.domain.CodeEntity;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityLevel;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.shared.id.IdFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Neo4jGraphAdaptersTests {

  private static final String SOURCE_A = "it-graph-a";
  private static final String SOURCE_B = "it-graph-b";

  @Autowired private CodeEntityRepository entities;
  @Autowired private RelationshipRepository relationships;
  @Autowired private EntityResolver resolver;
  @Autowired private Neo4jClient neo4j;

  @AfterEach
  void cleanUp() {
    relationships.deleteBySource(SOURCE_A);
    relationships.deleteBySource(SOURCE_B);
    entities.deleteBySource(SOURCE_A);
    entities.deleteBySource(SOURCE_B);
  }

  private static CodeEntity type(String source, String path, String simpleName, String qualified) {
    return new CodeEntity(
        IdFactory.entityId(source, path, qualified),
        source,
        IdFactory.fileId(source, path),
        null,
        CodeEntityLevel.CLASS,
        simpleName,
        qualified,
        "class " + simpleName,
        1,
        10);
  }

  private long count(String cypher) {
    return neo4j.query(cypher).fetchAs(Long.class).one().orElse(0L);
  }

  @Test
  void upsertsAStructuralEdgeBetweenEntities() {
    CodeEntity caller = type(SOURCE_A, "Caller.java", "Caller", "com.a.Caller");
    CodeEntity callee = type(SOURCE_A, "Callee.java", "Callee", "com.a.Callee");
    entities.upsertAll(List.of(caller, callee));

    relationships.upsertAll(
        List.of(Relationship.structural(caller.entityId(), callee.entityId(), RelationType.CALLS)));

    assertThat(
            count(
                "MATCH (:CodeEntity {entity_id: '"
                    + caller.entityId()
                    + "'})-[r:CALLS]->(:CodeEntity {entity_id: '"
                    + callee.entityId()
                    + "'}) RETURN count(r)"))
        .isEqualTo(1);
  }

  @Test
  void reUpsertIsIdempotent() {
    CodeEntity a = type(SOURCE_A, "A.java", "A", "com.a.A");
    CodeEntity b = type(SOURCE_A, "B.java", "B", "com.a.B");
    entities.upsertAll(List.of(a, b));
    Relationship edge = Relationship.structural(a.entityId(), b.entityId(), RelationType.EXTENDS);

    relationships.upsertAll(List.of(edge));
    relationships.upsertAll(List.of(edge));

    assertThat(count("MATCH ()-[r:EXTENDS]->() RETURN count(r)")).isEqualTo(1);
  }

  @Test
  void resolvesQualifiedNameAcrossSources() {
    CodeEntity inB = type(SOURCE_B, "Shared.java", "Shared", "com.shared.Shared");
    entities.upsertAll(List.of(inB));

    // A reference seen in source A resolves to the entity that lives in source B.
    Optional<String> resolved =
        resolver.resolve("com.shared.Shared", new ResolutionScope(SOURCE_A));

    assertThat(resolved).contains(inB.entityId());
  }

  @Test
  void prefersSameSourceOnNameCollision() {
    CodeEntity inA = type(SOURCE_A, "Dup.java", "Dup", "com.a.Dup");
    CodeEntity inB = type(SOURCE_B, "Dup.java", "Dup", "com.b.Dup");
    entities.upsertAll(List.of(inA, inB));

    List<String> byName = resolver.findByName("Dup", new ResolutionScope(SOURCE_A));

    assertThat(byName).startsWith(inA.entityId());
  }

  @Test
  void deleteBySourceDropsManagedEdgesButKeepsHierarchy() {
    CodeEntity parent = type(SOURCE_A, "P.java", "P", "com.a.P");
    CodeEntity child =
        new CodeEntity(
            IdFactory.entityId(SOURCE_A, "P.java", "com.a.P#m"),
            SOURCE_A,
            IdFactory.fileId(SOURCE_A, "P.java"),
            parent.entityId(),
            CodeEntityLevel.METHOD,
            "m",
            "com.a.P#m",
            "void m()",
            2,
            4);
    entities.upsertAll(List.of(parent, child));
    relationships.upsertAll(
        List.of(Relationship.structural(child.entityId(), parent.entityId(), RelationType.CALLS)));

    relationships.deleteBySource(SOURCE_A);

    assertThat(count("MATCH ()-[r:CALLS]->() RETURN count(r)")).isZero();
    // The DECLARES/CONTAINS hierarchy written by indexing is left intact.
    assertThat(count("MATCH (:CodeEntity)-[r:CONTAINS]->(:CodeEntity) RETURN count(r)"))
        .isEqualTo(1);
  }
}
