package com.schakir.hdb.core.web.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.schakir.hdb.core.web.CorrelationId;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.io.File;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Drives the shared technical error handling (ADR 0037) through the real application context, so
 * the real Jackson configuration (snake_case, strict unknown properties) is exercised, not a
 * hand-built one. Uses {@code MockMvcBuilders.webAppContextSetup} directly instead of
 * {@code @AutoConfigureMockMvc}: that annotation now lives in the {@code spring-boot-webmvc-test}
 * module, which is not part of this ticket's approved dependencies (Spring Boot 4 split it out of
 * {@code spring-boot-test-autoconfigure}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
class ProblemDetailsContractTests {

  @Autowired private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

  private static Schema problemDetailsSchema;

  @BeforeAll
  static void loadProblemDetailsSchemaFromContract() {
    File contractFile = new File("../../contracts/openapi.yaml");
    JsonNode contract = new YAMLMapper().readTree(contractFile);
    JsonNode schemaNode = contract.at("/components/schemas/ProblemDetails");
    assertThat(schemaNode.isMissingNode())
        .as("components.schemas.ProblemDetails must exist in contracts/openapi.yaml")
        .isFalse();

    SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    problemDetailsSchema = registry.getSchema(schemaNode);
  }

  @Test
  void theLoadedSchemaActuallyRejectsAnInvalidBody() {
    JsonNode missingType = new ObjectMapper().readTree("{\"title\":\"x\",\"status\":400}");
    assertThat(problemDetailsSchema.validate(missingType)).isNotEmpty();

    JsonNode wrongStatusType =
        new ObjectMapper()
            .readTree("{\"type\":\"/problems/x\",\"title\":\"x\",\"status\":\"400\"}");
    assertThat(problemDetailsSchema.validate(wrongStatusType)).isNotEmpty();
  }

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void invalidBodyReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/test-only/samples/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(
            result ->
                assertType(result.getResponse().getContentAsString(), "/problems/validation-error"))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()))
        .andExpect(result -> assertNoInstance(result.getResponse().getContentAsString()));
  }

  @Test
  void unknownPropertyReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/test-only/samples/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"ok\", \"unexpected_field\": true}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(
            result ->
                assertType(result.getResponse().getContentAsString(), "/problems/validation-error"))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  @Test
  void nonUuidPathParameterReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/test-only/samples/not-a-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"ok\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(
            result ->
                assertType(result.getResponse().getContentAsString(), "/problems/validation-error"))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  @Test
  void unhandledExceptionReturnsAboutBlankWithNoTechnicalDetail() throws Exception {
    mockMvc
        .perform(
            post("/test-only/samples/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"boom\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(result -> assertType(result.getResponse().getContentAsString(), "about:blank"))
        .andExpect(
            result -> {
              String body = result.getResponse().getContentAsString();
              assertThat(body).doesNotContain("IllegalStateException");
              assertThat(body).doesNotContain("simulated failure");
              assertThat(body).doesNotContain("com.schakir.hdb");
              assertValidAgainstProblemDetailsSchema(body);
            });
  }

  @Test
  void instanceReflectsTheCorrelationIdWhenSet() throws Exception {
    MDC.put(CorrelationId.MDC_KEY, "11111111-1111-1111-1111-111111111111");
    try {
      mockMvc
          .perform(
              post("/test-only/samples/" + UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\": \"\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(
              result -> {
                String body = result.getResponse().getContentAsString();
                assertThat(body).contains("11111111-1111-1111-1111-111111111111");
                assertValidAgainstProblemDetailsSchema(body);
              });
    } finally {
      MDC.remove(CorrelationId.MDC_KEY);
    }
  }

  @Test
  void missingRequestParameterGetsAnExplicitTypeAndNoInstance() throws Exception {
    mockMvc
        .perform(get("/test-only/samples/by-name"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(
            result ->
                assertType(result.getResponse().getContentAsString(), "/problems/validation-error"))
        .andExpect(result -> assertNoInstance(result.getResponse().getContentAsString()))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  @Test
  void rawResponseStatusExceptionGetsAnExplicitTypeAndNoInstance() throws Exception {
    mockMvc
        .perform(get("/test-only/samples/conflict"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(result -> assertType(result.getResponse().getContentAsString(), "about:blank"))
        .andExpect(result -> assertNoInstance(result.getResponse().getContentAsString()))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  @Test
  void methodNotAllowedCarriesTheAllowHeader() throws Exception {
    mockMvc
        .perform(put("/test-only/samples/" + UUID.randomUUID()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("POST")))
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(result -> assertType(result.getResponse().getContentAsString(), "about:blank"))
        .andExpect(result -> assertNoInstance(result.getResponse().getContentAsString()))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  private static void assertType(String body, String expected) {
    JsonNode node = new ObjectMapper().readTree(body);
    assertThat(node.path("type").asString()).isEqualTo(expected);
  }

  private static void assertValidAgainstProblemDetailsSchema(String body) {
    JsonNode node = new ObjectMapper().readTree(body);
    List<Error> errors = problemDetailsSchema.validate(node);
    assertThat(errors).as("ProblemDetails schema violations: %s", errors).isEmpty();
  }

  private static void assertNoInstance(String body) {
    JsonNode node = new ObjectMapper().readTree(body);
    assertThat(node.has("instance")).isFalse();
  }
}
