package com.knowledgehub.access.infrastructure.persistence;

import com.knowledgehub.access.domain.GrantRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link GrantRepository}. A grant is a {@code GRANTED} edge to a {@code :Source}; the
 * effective readable set is the union over the principal and every group it belongs to, resolved by
 * a single variable-length {@code MEMBER_OF*0..} traversal.
 */
@Component
class Neo4jGrantAdapter implements GrantRepository {

  private static final String GRANT =
      "MATCH (p:Principal {principal_id: $id})"
          + " UNWIND $sourceIds AS sid"
          + " MATCH (s:Source {source_id: sid})"
          + " MERGE (p)-[g:GRANTED]->(s) SET g.permission = 'read'";

  private static final String REVOKE =
      "MATCH (p:Principal {principal_id: $id})-[g:GRANTED]->(s:Source)"
          + " WHERE s.source_id IN $sourceIds DELETE g";

  private static final String GRANTED_SOURCES =
      "MATCH (p:Principal {principal_id: $id})-[:GRANTED]->(s:Source)"
          + " RETURN s.source_id AS id ORDER BY id";

  private static final String READABLE_FOR =
      "MATCH (p:Principal {principal_id: $id})"
          + " OPTIONAL MATCH (p)-[:MEMBER_OF*0..]->(g:Principal)-[:GRANTED]->(s:Source)"
          + " RETURN collect(DISTINCT s.source_id) AS ids";

  private static final String GRANTING_PRINCIPALS =
      "MATCH (p:Principal {principal_id: $id})-[:MEMBER_OF*0..]->(g:Principal)-[:GRANTED]->"
          + "(s:Source) RETURN s.source_id AS source, collect(DISTINCT g.principal_id) AS via";

  private static final String ALL_GRANTED =
      "MATCH (:Principal)-[:GRANTED]->(s:Source) RETURN collect(DISTINCT s.source_id) AS ids";

  private final Neo4jClient client;

  Neo4jGrantAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public void grant(String principalId, Collection<String> sourceIds) {
    if (sourceIds.isEmpty()) {
      return;
    }
    client
        .query(GRANT)
        .bindAll(Map.of("id", principalId, "sourceIds", List.copyOf(sourceIds)))
        .run();
  }

  @Override
  public void revoke(String principalId, Collection<String> sourceIds) {
    if (sourceIds.isEmpty()) {
      return;
    }
    client
        .query(REVOKE)
        .bindAll(Map.of("id", principalId, "sourceIds", List.copyOf(sourceIds)))
        .run();
  }

  @Override
  public List<String> grantedSources(String principalId) {
    return client
        .query(GRANTED_SOURCES)
        .bind(principalId)
        .to("id")
        .fetchAs(String.class)
        .mappedBy((t, row) -> row.get("id").asString())
        .all()
        .stream()
        .toList();
  }

  @Override
  public Set<String> readableSourcesFor(String principalId) {
    return client
        .query(READABLE_FOR)
        .bind(principalId)
        .to("id")
        .fetch()
        .one()
        .<Set<String>>map(row -> new LinkedHashSet<>(asStrings(row.get("ids"))))
        .orElseGet(LinkedHashSet::new);
  }

  @Override
  public Map<String, Set<String>> grantingPrincipalsFor(String principalId) {
    Map<String, Set<String>> bySource = new java.util.LinkedHashMap<>();
    client
        .query(GRANTING_PRINCIPALS)
        .bind(principalId)
        .to("id")
        .fetch()
        .all()
        .forEach(
            row ->
                bySource.put(
                    (String) row.get("source"), new LinkedHashSet<>(asStrings(row.get("via")))));
    return bySource;
  }

  @Override
  public Set<String> allGrantedSources() {
    return client
        .query(ALL_GRANTED)
        .fetch()
        .one()
        .<Set<String>>map(row -> new LinkedHashSet<>(asStrings(row.get("ids"))))
        .orElseGet(LinkedHashSet::new);
  }

  @SuppressWarnings("unchecked")
  private static List<String> asStrings(Object value) {
    return value == null ? List.of() : (List<String>) value;
  }
}
