package com.knowledgehub.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Centralizes the REST API version prefix in one place. Every {@code @RestController} under {@code
 * com.knowledgehub} is served under {@code /api/v1}, so controllers declare only their resource
 * path (e.g. {@code /system}, {@code /sources}). Bumping to {@code /api/v2} or running two versions
 * side by side is a one-line change here.
 *
 * <p>Scoped by package + {@code @RestController} so it does not touch {@code /actuator}, {@code
 * /docs} (springdoc), or the MCP endpoint at {@code /mcp}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private static final String API_PREFIX = "/api/v1";

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        API_PREFIX,
        controller ->
            controller.isAnnotationPresent(RestController.class)
                && controller.getPackageName().startsWith("com.knowledgehub"));
  }
}
