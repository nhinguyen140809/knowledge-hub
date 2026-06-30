package com.knowledgehub.access.infrastructure.retention;

import com.knowledgehub.access.domain.CredentialRepository;
import com.knowledgehub.shared.config.AppProperties;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hard-deletes revoked credentials once they are older than the configured retention window. Revoke
 * is a soft-delete that keeps the audit trail, so this periodic purge is what stops revoked nodes
 * from accumulating forever. Runs daily.
 */
@Component
class CredentialRetentionJob {

  private static final Logger log = LoggerFactory.getLogger(CredentialRetentionJob.class);

  private final CredentialRepository credentials;
  private final AppProperties properties;

  CredentialRetentionJob(CredentialRepository credentials, AppProperties properties) {
    this.credentials = credentials;
    this.properties = properties;
  }

  @Scheduled(cron = "${app.security.retention-cron:0 0 3 * * *}")
  void purgeExpiredRevokedCredentials() {
    int months = properties.security().credentialRetentionMonths();
    Instant cutoff = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(months).toInstant();
    int purged = credentials.purgeRevokedBefore(cutoff);
    if (purged > 0) {
      log.info("Purged {} revoked credentials older than {} months", purged, months);
    }
  }
}
