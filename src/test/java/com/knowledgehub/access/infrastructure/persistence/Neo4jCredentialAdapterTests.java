package com.knowledgehub.access.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.access.domain.Credential;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.access.domain.port.CredentialRepository;
import com.knowledgehub.access.domain.port.PrincipalRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Retention purges by revocation time, not creation time: a credential created long ago but
 * revoked recently must survive a purge; one revoked long ago must not, regardless of its age.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Neo4jCredentialAdapterTests {

  private static final String PRINCIPAL_ID = "cred-it-principal";
  private static final String KEEP_ID = "cred-it-keep";
  private static final String PURGE_ID = "cred-it-purge";
  private static final String ACTIVE_ID = "cred-it-active";

  @Autowired private CredentialRepository credentials;
  @Autowired private PrincipalRepository principals;

  @BeforeEach
  void setUp() {
    principals.save(new Principal(PRINCIPAL_ID, PrincipalType.SUBJECT, Role.MEMBER));
  }

  @AfterEach
  void tearDown() {
    // Sweep every credential this test may have created, revoked or not, past any backdated
    // fixture timestamp, then remove the principal itself.
    credentials.revoke(KEEP_ID, Instant.now());
    credentials.revoke(PURGE_ID, Instant.now());
    credentials.revoke(ACTIVE_ID, Instant.now());
    credentials.purgeRevokedBefore(Instant.now().plusSeconds(60));
    principals.deleteById(PRINCIPAL_ID);
  }

  @Test
  void purgeScansByRevocationTimeNotCreationTime() {
    Instant longAgo = Instant.now().minus(400, ChronoUnit.DAYS);
    // All three were created long ago; only revocation time should decide the outcome.
    credentials.save(KEEP_ID, PRINCIPAL_ID, "keep", "hash-" + KEEP_ID, longAgo);
    credentials.save(PURGE_ID, PRINCIPAL_ID, "purge", "hash-" + PURGE_ID, longAgo);
    credentials.save(ACTIVE_ID, PRINCIPAL_ID, "active", "hash-" + ACTIVE_ID, longAgo);

    credentials.revoke(KEEP_ID, Instant.now()); // revoked recently: must survive
    credentials.revoke(PURGE_ID, longAgo); // revoked long ago: must be purged
    // ACTIVE_ID stays un-revoked: must survive regardless of its age

    Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
    int purged = credentials.purgeRevokedBefore(cutoff);

    assertThat(purged).isGreaterThanOrEqualTo(1);
    assertThat(credentials.listByPrincipal(PRINCIPAL_ID))
        .extracting(Credential::credentialId)
        .containsExactlyInAnyOrder(KEEP_ID, ACTIVE_ID);
  }
}
