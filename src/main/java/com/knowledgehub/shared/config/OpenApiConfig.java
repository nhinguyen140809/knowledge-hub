package com.knowledgehub.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata and grouping for the {@code /docs} UI. Every endpoint is authenticated with a
 * bearer credential, so the spec declares a single bearer scheme applied globally — the "Authorize"
 * box in the UI sends {@code Authorization: Bearer <secret>}. The surface is split into two groups,
 * <em>query</em> (what an agent calls) and <em>admin</em> (what an operator calls), so each
 * audience reads only the endpoints it needs.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI knowledgeHubOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Knowledge Hub API")
                .version("v1")
                .description(
                    "Hybrid retrieval over a knowledge graph, plus source ingestion, sync, and "
                        + "access-control administration. Every request carries a bearer credential; "
                        + "queries are scoped to the caller's readable sources."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("Bearer credential issued by an administrator.")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }

  /** Query-side surface an agent consumes: retrieval and service status. */
  @Bean
  public GroupedOpenApi queryApi() {
    return GroupedOpenApi.builder()
        .group("query")
        .pathsToMatch("/api/v1/query", "/api/v1/query/**", "/api/v1/system/**")
        .build();
  }

  /** Administration surface: sources, sync, and access control. */
  @Bean
  public GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder().group("admin").pathsToMatch("/api/v1/admin/**").build();
  }
}
