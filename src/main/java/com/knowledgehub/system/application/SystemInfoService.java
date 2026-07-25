package com.knowledgehub.system.application;

import com.knowledgehub.system.domain.SystemInfo;
import com.knowledgehub.system.domain.port.SystemSettings;
import java.util.List;
import java.util.Optional;
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
  private final SystemSettings settings;

  public SystemInfoService(
      Environment environment,
      ObjectProvider<BuildProperties> buildProperties,
      SystemSettings settings) {
    this.environment = environment;
    this.buildProperties = buildProperties;
    this.settings = settings;
  }

  /** Returns a snapshot of the service's current runtime information. */
  public SystemInfo currentInfo() {
    String application = environment.getProperty("spring.application.name", "knowledge-hub");
    BuildProperties build = buildProperties.getIfAvailable();
    String version = build != null ? build.getVersion() : "unknown";
    List<String> profiles = List.of(environment.getActiveProfiles());
    String productName = resolveProductName(application);

    log.debug(
        "Reporting system info: productName={}, application={}, version={}, profiles={}",
        productName,
        application,
        version,
        profiles);

    return new SystemInfo(productName, application, version, profiles);
  }

  /** Sets the operator-facing product name; it takes effect immediately for later reads. */
  public void setProductName(String productName) {
    settings.setProductName(productName);
  }

  /**
   * The display product name in precedence order: an operator's stored override, then the
   * configured {@code app.product-name}, then the technical application name as a last resort.
   */
  private String resolveProductName(String application) {
    return settings
        .productName()
        .or(() -> Optional.ofNullable(environment.getProperty("app.product-name")))
        .filter(name -> !name.isBlank())
        .orElse(application);
  }
}
