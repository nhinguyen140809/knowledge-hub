package com.knowledgehub.access.infrastructure.persistence;

import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalRepository;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.neo4j.driver.Record;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/** Neo4j-backed {@link PrincipalRepository}. Principals and group membership live as nodes/edges. */
@Component
class Neo4jPrincipalAdapter implements PrincipalRepository {

  private static final String SAVE =
      "MERGE (p:Principal {principal_id: $id}) SET p.type = $type, p.role = $role";

  private static final String FIND =
      "MATCH (p:Principal {principal_id: $id})"
          + " RETURN p.principal_id AS id, p.type AS type, p.role AS role";

  private static final String FIND_ALL =
      "MATCH (p:Principal) RETURN p.principal_id AS id, p.type AS type, p.role AS role"
          + " ORDER BY p.principal_id";

  private static final String DELETE = "MATCH (p:Principal {principal_id: $id}) DETACH DELETE p";

  private static final String EXISTS_BY_ROLE =
      "MATCH (p:Principal {role: $role}) RETURN count(p) > 0 AS present";

  private static final String ADD_MEMBER =
      "MATCH (m:Principal {principal_id: $memberId}), (g:Principal {principal_id: $groupId})"
          + " MERGE (m)-[:MEMBER_OF]->(g)";

  private static final String REMOVE_MEMBER =
      "MATCH (m:Principal {principal_id: $memberId})-[r:MEMBER_OF]->"
          + "(g:Principal {principal_id: $groupId}) DELETE r";

  private static final String MEMBERS_OF =
      "MATCH (m:Principal)-[:MEMBER_OF]->(g:Principal {principal_id: $groupId})"
          + " RETURN m.principal_id AS id ORDER BY m.principal_id";

  private final Neo4jClient client;

  Neo4jPrincipalAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public Principal save(Principal principal) {
    client
        .query(SAVE)
        .bindAll(
            Map.of(
                "id",
                principal.principalId(),
                "type",
                principal.type().name(),
                "role",
                principal.role().name()))
        .run();
    return principal;
  }

  @Override
  public Optional<Principal> findById(String principalId) {
    return client
        .query(FIND)
        .bind(principalId)
        .to("id")
        .fetchAs(Principal.class)
        .mappedBy((t, row) -> toPrincipal(row))
        .one();
  }

  @Override
  public List<Principal> findAll() {
    return client.query(FIND_ALL).fetchAs(Principal.class).mappedBy((t, row) -> toPrincipal(row))
        .all().stream().toList();
  }

  @Override
  public void deleteById(String principalId) {
    client.query(DELETE).bind(principalId).to("id").run();
  }

  @Override
  public boolean existsByRole(Role role) {
    return client
        .query(EXISTS_BY_ROLE)
        .bind(role.name())
        .to("role")
        .fetchAs(Boolean.class)
        .mappedBy((t, row) -> row.get("present").asBoolean())
        .one()
        .orElse(false);
  }

  @Override
  public void addMember(String groupId, String memberId) {
    client.query(ADD_MEMBER).bindAll(Map.of("groupId", groupId, "memberId", memberId)).run();
  }

  @Override
  public void removeMember(String groupId, String memberId) {
    client.query(REMOVE_MEMBER).bindAll(Map.of("groupId", groupId, "memberId", memberId)).run();
  }

  @Override
  public List<String> membersOf(String groupId) {
    return client.query(MEMBERS_OF).bind(groupId).to("groupId").fetchAs(String.class)
        .mappedBy((t, row) -> row.get("id").asString())
        .all().stream().toList();
  }

  private static Principal toPrincipal(Record row) {
    return new Principal(
        row.get("id").asString(),
        PrincipalType.valueOf(row.get("type").asString()),
        Role.valueOf(row.get("role").asString()));
  }
}
