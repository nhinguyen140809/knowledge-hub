package com.knowledgehub.system.application;

import com.knowledgehub.system.domain.SystemInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Assembles runtime information about the running service from the environment and config. */
@Service
public class SystemInfoService {

  private static final Logger log = LoggerFactory.getLogger(SystemInfoService.class);

  private final Environment environment;
  private final ObjectProvider<BuildProperties> buildProperties;

  public SystemInfoService(
      Environment environment, ObjectProvider<BuildProperties> buildProperties) {
    this.environment = environment;
    this.buildProperties = buildProperties;
  }

  /** Returns a snapshot of the service's current runtime information. */
  public SystemInfo currentInfo() {
    String application = environment.getProperty("spring.application.name", "knowledge-hub");
    BuildProperties build = buildProperties.getIfAvailable();
    String version = build != null ? build.getVersion() : "unknown";
    List<String> profiles = List.of(environment.getActiveProfiles());

    log.debug(
        "Reporting system info: application={}, version={}, profiles={}",
        application,
        version,
        profiles);

    return new SystemInfo(application, version, profiles);
  }
}
