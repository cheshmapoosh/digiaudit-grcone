package com.digiaudit.grcpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

/** Explicit opt-in verification against a disposable Oracle schema, never the application schema. */
@SpringBootTest(properties = {"app.minio.enabled=false", "spring.main.banner-mode=off"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "GRCPC_VERIFY_ORACLE", matches = "true")
@EnabledIfEnvironmentVariable(named = "DB_SCHEMA", matches = "GRCPC_V_[A-Z0-9_]+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GovernanceCatalogOracleVerificationTest {
  private static final String PROFILE = "00000000-0000-4000-8000-000000000001";
  private static final String CONTROLS = "/api/master-data/central/controls";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoSpyBean com.digiaudit.grcpc.modules.masterdata.security.MasterDataAuthorizationService authorization;
  private MockHttpSession session;

  @BeforeAll
  void initializeDisposableSystem() throws Exception {
    var state = request(HttpMethod.GET, "/api/setup/status", null, 200);
    String password = System.getenv("DB_PASSWORD");
    assertThat(password).isNotBlank();
    if (!state.path("initialized").asBoolean()) {
      request(HttpMethod.POST, "/api/setup/initialize", Map.of(
          "username", "verification", "password", password,
          "firstName", "Verification", "lastName", "Root"), 204);
    }
    var login = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(Map.of("username", "verification", "password", password))))
        .andReturn();
    assertThat(login.getResponse().getStatus()).isEqualTo(204);
    session = (MockHttpSession) login.getRequest().getSession(false);
    assertThat(session).isNotNull();
  }

  @Test
  void createsCentralControlWithOptionalFieldsAndReadsItBack() throws Exception {
    var group = request(HttpMethod.POST, "/api/master-data/central/control-groups",
        Map.of("code", code(), "title", "گروه کنترل آزمایشی", "sortOrder", 0), 200);
    var body = new LinkedHashMap<String, Object>();
    body.put("code", code());
    body.put("title", "کنترل آزمایشی");
    body.put("controlGroupId", group.path("entityId").asText());
    body.put("controlClass", "ACTIVITY_LEVEL");
    body.put("importance", "PRIMARY");
    body.put("controlRisk", "LOW");
    body.put("automationType", "MANUAL");
    body.put("controlPurpose", "PREVENTIVE");
    body.put("nature", "REVIEW");
    body.put("triggerType", "DATE");
    body.put("operationFrequency", "DAILY");
    body.put("validFrom", "2026-01-01");
    body.put("validTo", "2026-12-31");
    var created = request(HttpMethod.POST, CONTROLS, body, 200);
    var detail = request(HttpMethod.GET, CONTROLS + "/" + created.path("entityId").asText(), null, 200);
    assertThat(detail.path("title").asText()).isEqualTo(body.get("title"));
    assertThat(detail.path("controlClass").asText()).isEqualTo("ACTIVITY_LEVEL");
    assertThat(detail.path("validFrom").asText()).isEqualTo("2026-01-01");
    body.put("code", code());
    body.put("controlClass", null);
    body.put("importance", null);
    body.put("triggerType", null);
    body.put("operationFrequency", null);
    var cleared = request(HttpMethod.POST, CONTROLS, body, 200);
    assertThat(cleared.path("entity").path("controlClass").isNull()).isTrue();
  }

  @Test
  void createsGlobalControlAndIncludesItInTheList() throws Exception {
    var group = request(HttpMethod.POST, "/api/control-groups",
        Map.of("code", code(), "name", "گروه کنترل سطح کل آزمایشی"), 200);
    var created = request(HttpMethod.POST, "/api/global-controls", Map.of(
        "code", code(), "name", "کنترل سطح کل آزمایشی",
        "controlGroupId", group.path("entityId").asText(), "controlType", "سطح کل",
        "testRequired", false, "validFrom", "2026-01-01", "validTo", "2026-12-31"), 200);
    var detail = request(HttpMethod.GET, "/api/global-controls/" + created.path("entityId").asText(), null, 200);
    assertThat(detail.path("name").asText()).isEqualTo("کنترل سطح کل آزمایشی");
    var list = request(HttpMethod.GET, "/api/global-controls", null, 200);
    assertThat(list.findValuesAsText("id")).contains(created.path("entityId").asText());
  }

  @Test
  void requiresProcessManagementOnlyWhenCreatingWithAnInitialSubprocess() throws Exception {
    var group = request(HttpMethod.POST, "/api/master-data/central/control-groups",
        Map.of("code", code(), "title", "گروه آزمون مجوز", "sortOrder", 0), 200);
    var body = new LinkedHashMap<String, Object>();
    body.put("code", code());
    body.put("title", "کنترل آزمون مجوز");
    body.put("controlGroupId", group.path("entityId").asText());
    body.put("initialSubprocess", Map.of("subprocessId", UUID.randomUUID().toString(), "version", 1));
    doReturn(false).when(authorization).canManage("PROCESS");
    try {
      request(HttpMethod.POST, CONTROLS, body, 403);
      assertThat(request(HttpMethod.GET, CONTROLS, null, 200).findValuesAsText("code")).doesNotContain((String) body.get("code"));
      body.remove("initialSubprocess");
      request(HttpMethod.POST, CONTROLS, body, 200);
    } finally {
      doCallRealMethod().when(authorization).canManage("PROCESS");
    }
  }

  @Test
  void reservesGlobalControlCodesAfterDeletionAndRestoresTheSameRecord() throws Exception {
    var group = request(HttpMethod.POST, "/api/control-groups",
        Map.of("code", code(), "name", "گروه آزمون کد تکراری"), 200);
    String code = code();
    var body = Map.of("code", code, "name", "کنترل آزمون کد تکراری",
        "controlGroupId", group.path("entityId").asText(), "controlType", "سطح کل", "testRequired", false);
    var created = request(HttpMethod.POST, "/api/global-controls", body, 200);
    var duplicate = request(HttpMethod.POST, "/api/global-controls", body, 409);
    assertThat(duplicate.path("code").asText()).isEqualTo("DUPLICATE_BUSINESS_KEY");
    assertThat(duplicate.path("message").asText()).contains(code, "قبلاً");
    String id = created.path("entityId").asText();
    var deleted = request(HttpMethod.DELETE, "/api/global-controls/" + id + "?version="
        + created.path("version").asLong(), null, 200);
    assertThat(request(HttpMethod.GET, "/api/global-controls", null, 200).findValuesAsText("id")).doesNotContain(id);
    request(HttpMethod.POST, "/api/global-controls", body, 409);
    request(HttpMethod.POST, "/api/global-controls/" + id + "/restore",
        Map.of("version", deleted.path("version").asLong()), 200);
    assertThat(request(HttpMethod.GET, "/api/global-controls/" + id, null, 200).path("code").asText()).isEqualTo(code);
  }

  @Test
  void preservesStrategyDatesOnCreateUpdateAndRead() throws Exception {
    var created = request(HttpMethod.POST, "/api/objectives", Map.of(
        "kind", "STRATEGY", "code", code(), "name", "استراتژی آزمایشی",
        "validFrom", "2026-01-01", "validTo", "2027-12-31"), 200);
    assertThat(created.path("objective").path("validFrom").asText()).isEqualTo("2026-01-01");
    String url = "/api/objectives/" + created.path("entityId").asText();
    var updated = request(HttpMethod.PUT, url, Map.of(
        "kind", "STRATEGY", "name", "استراتژی آزمایشی",
        "validFrom", "2026-02-01", "validTo", "2028-12-31",
        "version", created.path("version").asLong()), 200);
    assertThat(updated.path("objective").path("validTo").asText()).isEqualTo("2028-12-31");
    assertThat(request(HttpMethod.GET, url, null, 200).path("validFrom").asText()).isEqualTo("2026-02-01");
  }

  @Test
  void rejectsAnInvertedStrategyDateRangeWithoutCreatingIt() throws Exception {
    String code = code();
    request(HttpMethod.POST, "/api/objectives", Map.of("kind", "STRATEGY", "code", code,
        "name", "بازه نامعتبر", "validFrom", "2027-01-01", "validTo", "2026-01-01"), 422);
    assertThat(request(HttpMethod.GET, "/api/objectives", null, 200).findValuesAsText("code")).doesNotContain(code);
  }

  private JsonNode request(HttpMethod method, String url, Object body, int expected) throws Exception {
    var builder = MockMvcRequestBuilders.request(method, url).servletPath(java.net.URI.create(url).getPath())
        .header("X-Profile-Id", PROFILE).header("Accept-Language", "fa");
    if (session != null) builder.session(session);
    if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
    var response = mvc.perform(builder).andReturn().getResponse();
    assertThat(response.getStatus()).as("%s %s: %s", method, url, response.getContentAsString()).isEqualTo(expected);
    return response.getContentAsByteArray().length == 0 ? json.nullNode() : json.readTree(response.getContentAsByteArray());
  }

  private String code() { return "VERIFY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(); }
}
