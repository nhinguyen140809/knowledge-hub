package com.knowledgehub.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTests {

  private final OpenApiConfig config = new OpenApiConfig();

  @Test
  void declaresASingleBearerSchemeAppliedGlobally() {
    OpenAPI openApi = config.knowledgeHubOpenApi();

    assertThat(openApi.getInfo().getTitle()).isEqualTo("Knowledge Hub API");
    SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get("bearerAuth");
    assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
    assertThat(scheme.getScheme()).isEqualTo("bearer");
    // Applied to the whole API so the docs "Authorize" box covers every endpoint.
    assertThat(openApi.getSecurity()).anySatisfy(req -> assertThat(req).containsKey("bearerAuth"));
  }

  @Test
  void splitsTheSurfaceIntoQueryAndAdminGroups() {
    assertThat(config.queryApi().getGroup()).isEqualTo("query");
    assertThat(config.queryApi().getPathsToMatch()).contains("/api/v1/query");
    assertThat(config.adminApi().getGroup()).isEqualTo("admin");
    assertThat(config.adminApi().getPathsToMatch()).containsExactly("/api/v1/admin/**");
  }
}
