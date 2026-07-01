package com.knowledgehub.access;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.PrincipalRepository;
import com.knowledgehub.access.domain.SystemConfigRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.sync.infrastructure.mcp.SyncSourceTools;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end access control over the real security filter chain and Neo4j. A bootstrap admin is
 * seeded from the configured key; from there the test drives the admin API to manage principals,
 * groups, grants, policy and credentials, and checks the authentication/authorization boundary:
 * no/invalid token is rejected, admin operations need an admin, a revoked credential stops working
 * immediately, and effective permissions union group grants under both policies.
 */
@SpringBootTest(properties = "app.security.api-key=" + AccessControlIntegrationTests.ADMIN_KEY)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccessControlIntegrationTests {

  static final String ADMIN_KEY = "bootstrap-secret";

  private static final String MEMBER = "acl-member";
  private static final String GROUP = "acl-group";
  private static final String USER = "acl-user";
  private static final String RESTRICTED_OWNER = "acl-owner";
  private static final String SRC_A = "acl-src-a";
  private static final String SRC_B = "acl-src-b";

  @MockitoBean private EmbeddingModel embeddingModel;

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private PrincipalRepository principals;
  @Autowired private SourceRepository sources;
  @Autowired private SystemConfigRepository systemConfig;
  @Autowired private SyncSourceTools syncSourceTools;

  @BeforeEach
  void setUp() {
    when(embeddingModel.embed(anyList()))
        .thenAnswer(
            invocation -> {
              List<String> texts = invocation.getArgument(0);
              return texts.stream().map(t -> new float[1536]).toList();
            });
    sources.save(new Source(SRC_A, SourceType.FS, "/a", null, List.of(), List.of()));
    sources.save(new Source(SRC_B, SourceType.FS, "/b", null, List.of(), List.of()));
  }

  @AfterEach
  void tearDown() {
    for (String id : List.of(MEMBER, GROUP, USER, RESTRICTED_OWNER)) {
      principals.deleteById(id);
    }
    sources.deleteById(SRC_A);
    sources.deleteById(SRC_B);
    systemConfig.setDefaultPolicy(DefaultPolicy.DENY);
  }

  @Test
  void rejectsRequestsWithoutAValidToken() throws Exception {
    mvc.perform(get("/api/v1/admin/principals"))
        .andExpect(status().isUnauthorized())
        // Even an auth failure carries a trace id, so a rejected request is reconstructable.
        .andExpect(jsonPath("$.traceId").exists());
    mvc.perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"anything\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/admin/principals").header("Authorization", "Bearer wrong"))
        .andExpect(status().isUnauthorized());
    // The MCP endpoint runs through the same filter chain, so it is closed to anonymous callers.
    mvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void theSyncMcpToolIsForbiddenToANonAdmin() {
    // Syncing is admin-only over REST; the MCP tool carries the same guard, so a member is denied
    // before the sync ever runs.
    assertThatThrownBy(() -> syncSourceTools.syncSource(SRC_A))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void adminManagesPrincipalsWhileAMemberIsForbiddenButCanQuery() throws Exception {
    createPrincipal(MEMBER, "SUBJECT", "MEMBER");
    String memberToken = issueCredential(MEMBER);

    // A member may run a query (any authenticated principal can).
    mvc.perform(
            post("/api/v1/query")
                .header("Authorization", bearer(memberToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hello\"}"))
        .andExpect(status().isOk());

    // But not touch the admin API.
    mvc.perform(get("/api/v1/admin/principals").header("Authorization", bearer(memberToken)))
        .andExpect(status().isForbidden());
    // The admin can.
    mvc.perform(get("/api/v1/admin/principals").header("Authorization", bearer(ADMIN_KEY)))
        .andExpect(status().isOk());
  }

  @Test
  void revokingACredentialBlocksTheVeryNextRequest() throws Exception {
    createPrincipal(USER, "SUBJECT", "MEMBER");
    MvcResult issued =
        mvc.perform(
                post("/api/v1/admin/principals/" + USER + "/credentials")
                    .header("Authorization", bearer(ADMIN_KEY)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = json.readTree(issued.getResponse().getContentAsString());
    String token = body.get("secret").asText();
    String credentialId = body.get("credentialId").asText();

    mvc.perform(
            post("/api/v1/query")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hello\"}"))
        .andExpect(status().isOk());

    mvc.perform(
            delete("/api/v1/admin/credentials/" + credentialId)
                .header("Authorization", bearer(ADMIN_KEY)))
        .andExpect(status().isNoContent());

    mvc.perform(
            post("/api/v1/query")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hello\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void effectivePermissionsUnionGroupGrantsUnderDenyPolicy() throws Exception {
    createPrincipal(GROUP, "GROUP", "MEMBER");
    createPrincipal(USER, "SUBJECT", "MEMBER");
    addMember(GROUP, USER);
    grant(GROUP, SRC_A);

    // Under deny, the user reads only what it (or its groups) is granted: SRC_A via the group.
    mvc.perform(
            get("/api/v1/admin/principals/" + USER + "/effective-permissions")
                .header("Authorization", bearer(ADMIN_KEY)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.readableSources", containsInAnyOrder(SRC_A)))
        .andExpect(jsonPath("$.grantedVia['" + SRC_A + "']", hasItem(GROUP)));
  }

  @Test
  void allowPolicyReadsEverythingExceptRestrictedSources() throws Exception {
    createPrincipal(USER, "SUBJECT", "MEMBER");
    createPrincipal(RESTRICTED_OWNER, "SUBJECT", "MEMBER");
    grant(RESTRICTED_OWNER, SRC_A); // SRC_A now restricted to its owner
    setPolicy("ALLOW");

    // A user with no grants reads every source except the restricted SRC_A.
    mvc.perform(
            get("/api/v1/admin/principals/" + USER + "/effective-permissions")
                .header("Authorization", bearer(ADMIN_KEY)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.readableSources", hasItem(SRC_B)))
        .andExpect(jsonPath("$.readableSources", not(hasItem(SRC_A))));

    // The owner still reads the restricted source plus everything else.
    mvc.perform(
            get("/api/v1/admin/principals/" + RESTRICTED_OWNER + "/effective-permissions")
                .header("Authorization", bearer(ADMIN_KEY)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.readableSources", hasItem(SRC_A)))
        .andExpect(jsonPath("$.readableSources", hasItem(SRC_B)));
  }

  // --- helpers ---

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private void createPrincipal(String id, String type, String role) throws Exception {
    mvc.perform(
            post("/api/v1/admin/principals")
                .header("Authorization", bearer(ADMIN_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"principalId\":\"%s\",\"type\":\"%s\",\"role\":\"%s\"}"
                        .formatted(id, type, role)))
        .andExpect(status().isCreated());
  }

  private String issueCredential(String principalId) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/v1/admin/principals/" + principalId + "/credentials")
                    .header("Authorization", bearer(ADMIN_KEY)))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("secret").asText();
  }

  private void addMember(String groupId, String memberId) throws Exception {
    mvc.perform(
            post("/api/v1/admin/principals/" + groupId + "/members")
                .header("Authorization", bearer(ADMIN_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"%s\"}".formatted(memberId)))
        .andExpect(status().isNoContent());
  }

  private void grant(String principalId, String sourceId) throws Exception {
    mvc.perform(
            post("/api/v1/admin/grants")
                .header("Authorization", bearer(ADMIN_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"principalId\":\"%s\",\"sourceIds\":[\"%s\"]}"
                        .formatted(principalId, sourceId)))
        .andExpect(status().isNoContent());
  }

  private void setPolicy(String policy) throws Exception {
    mvc.perform(
            put("/api/v1/admin/default-policy")
                .header("Authorization", bearer(ADMIN_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"policy\":\"%s\"}".formatted(policy)))
        .andExpect(status().isOk());
  }
}
