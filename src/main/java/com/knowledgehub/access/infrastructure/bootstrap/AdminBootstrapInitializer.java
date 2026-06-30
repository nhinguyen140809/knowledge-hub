package com.knowledgehub.access.infrastructure.bootstrap;

import com.knowledgehub.access.application.Sha256;
import com.knowledgehub.access.domain.CredentialRepository;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalRepository;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.shared.config.AppProperties;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the first admin so the system can be administered before any credential exists in the
 * graph. On startup, if no admin principal is present and a bootstrap key is configured, it creates
 * an admin principal and registers the hash of that key as its credential. Idempotent: once an
 * admin exists it does nothing, so a restart never re-seeds. Every later credential lives in Neo4j,
 * not in config. Runs after the schema is created.
 */
@Component
@Order(100)
class AdminBootstrapInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

  private static final String ADMIN_ID = "bootstrap-admin";

  private final PrincipalRepository principals;
  private final CredentialRepository credentials;
  private final AppProperties properties;

  AdminBootstrapInitializer(
      PrincipalRepository principals, CredentialRepository credentials, AppProperties properties) {
    this.principals = principals;
    this.credentials = credentials;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    String apiKey = properties.security().apiKey();
    if (apiKey == null || apiKey.isBlank()) {
      log.info("No bootstrap API key configured; skipping admin seeding");
      return;
    }
    if (principals.existsByRole(Role.ADMIN)) {
      return;
    }
    principals.save(new Principal(ADMIN_ID, PrincipalType.SUBJECT, Role.ADMIN));
    credentials.save(UUID.randomUUID().toString(), ADMIN_ID, Sha256.hex(apiKey), Instant.now());
    log.info("Seeded bootstrap admin principal '{}' from the configured API key", ADMIN_ID);
  }
}
