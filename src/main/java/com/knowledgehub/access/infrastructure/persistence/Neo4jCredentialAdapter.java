package com.knowledgehub.access.infrastructure.persistence;

import com.knowledgehub.access.domain.Credential;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.access.domain.port.CredentialRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link CredentialRepository}. A credential is the SHA-256 hash of its secret on a
 * {@code :Credential} node linked to its principal; timestamps are epoch millis so the retention
 * job can range-scan them. The raw secret never reaches this layer.
 *
 * <p>{@code last_used_at} is on the authentication hot path, so it is only rewritten once the
 * stored value is older than {@link #LAST_USED_THROTTLE}: bursts of requests on the same credential
 * skip the write. This keeps the timestamp useful for audit without a write on every request.
 */
@Component
class Neo4jCredentialAdapter implements CredentialRepository {

  private static final Duration LAST_USED_THROTTLE = Duration.ofMinutes(5);

  private static final String SAVE =
      "MATCH (p:Principal {principal_id: $principalId})"
          + " CREATE (c:Credential {credential_id: $id, name: $name, hash: $hash, revoked: false,"
          + " created_at: $createdAt})"
          + " MERGE (p)-[:HAS_CREDENTIAL]->(c)";

  private static final String EXISTS_ACTIVE_BY_NAME =
      "MATCH (:Principal {principal_id: $principalId})-[:HAS_CREDENTIAL]"
          + "->(c:Credential {name: $name, revoked: false})"
          + " RETURN count(c) > 0 AS exists";

  private static final String FIND_PRINCIPAL_BY_HASH =
      "MATCH (c:Credential {hash: $hash, revoked: false})<-[:HAS_CREDENTIAL]-(p:Principal)"
          + " RETURN p.principal_id AS id, p.type AS type, p.role AS role";

  private static final String TOUCH_LAST_USED =
      "MATCH (c:Credential {hash: $hash})"
          + " WHERE c.last_used_at IS NULL OR c.last_used_at < $staleBefore"
          + " SET c.last_used_at = $when";

  private static final String REVOKE =
      "MATCH (c:Credential {credential_id: $id}) SET c.revoked = true";

  private static final String LIST_BY_PRINCIPAL =
      "MATCH (:Principal {principal_id: $principalId})-[:HAS_CREDENTIAL]->(c:Credential)"
          + " RETURN c.credential_id AS id, c.name AS name, c.revoked AS revoked,"
          + " c.created_at AS createdAt, c.last_used_at AS lastUsedAt ORDER BY c.created_at";

  private static final String PURGE_REVOKED =
      "MATCH (c:Credential {revoked: true}) WHERE c.created_at < $cutoff"
          + " DETACH DELETE c RETURN count(c) AS purged";

  private final Neo4jClient client;

  Neo4jCredentialAdapter(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public void save(
      String credentialId, String principalId, String name, String hash, Instant createdAt) {
    client
        .query(SAVE)
        .bindAll(
            Map.of(
                "id",
                credentialId,
                "principalId",
                principalId,
                "name",
                name,
                "hash",
                hash,
                "createdAt",
                createdAt.toEpochMilli()))
        .run();
  }

  @Override
  public boolean existsActiveByPrincipalAndName(String principalId, String name) {
    return client
        .query(EXISTS_ACTIVE_BY_NAME)
        .bindAll(Map.of("principalId", principalId, "name", name))
        .fetchAs(Boolean.class)
        .mappedBy((t, row) -> row.get("exists").asBoolean())
        .one()
        .orElse(false);
  }

  @Override
  public Optional<Principal> findActivePrincipalByHash(String hash) {
    return client
        .query(FIND_PRINCIPAL_BY_HASH)
        .bind(hash)
        .to("hash")
        .fetchAs(Principal.class)
        .mappedBy(
            (t, row) ->
                new Principal(
                    row.get("id").asString(),
                    PrincipalType.valueOf(row.get("type").asString()),
                    Role.valueOf(row.get("role").asString())))
        .one();
  }

  @Override
  public void touchLastUsed(String hash, Instant when) {
    long now = when.toEpochMilli();
    client
        .query(TOUCH_LAST_USED)
        .bindAll(
            Map.of("hash", hash, "when", now, "staleBefore", now - LAST_USED_THROTTLE.toMillis()))
        .run();
  }

  @Override
  public void revoke(String credentialId) {
    client.query(REVOKE).bind(credentialId).to("id").run();
  }

  @Override
  public List<Credential> listByPrincipal(String principalId) {
    return client
        .query(LIST_BY_PRINCIPAL)
        .bind(principalId)
        .to("principalId")
        .fetchAs(Credential.class)
        .mappedBy(
            (t, row) ->
                new Credential(
                    row.get("id").asString(),
                    row.get("name").asString(null),
                    row.get("revoked").asBoolean(),
                    Instant.ofEpochMilli(row.get("createdAt").asLong()),
                    instantOrNull(row.get("lastUsedAt"))))
        .all()
        .stream()
        .toList();
  }

  @Override
  public int purgeRevokedBefore(Instant cutoff) {
    Map<String, Object> params = new HashMap<>();
    params.put("cutoff", cutoff.toEpochMilli());
    return client
        .query(PURGE_REVOKED)
        .bindAll(params)
        .fetchAs(Long.class)
        .mappedBy((t, row) -> row.get("purged").asLong())
        .one()
        .orElse(0L)
        .intValue();
  }

  private static Instant instantOrNull(Value value) {
    return value == null || value.isNull() ? null : Instant.ofEpochMilli(value.asLong());
  }
}
