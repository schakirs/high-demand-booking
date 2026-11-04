package com.schakir.hdb.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.schakir.hdb.testsupport.SecurityTestSupport;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.io.File;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * End to end over real HTTP: the event created by {@code POST /v1/events} is read back identical by
 * {@code GET /v1/events/{id}} (same {@code created_at}), its {@code Location} resolves to it, and a
 * rule violation reaches the client as a valid problem response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
class EventRoundTripHttpTests extends SecurityTestSupport {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private JdbcTemplate jdbcTemplate;
  private MockMvc mockMvc;

  private static Schema problemDetailsSchema;

  @BeforeAll
  static void loadProblemDetailsSchema() {
    JsonNode contract = new YAMLMapper().readTree(new File("../../contracts/openapi.yaml"));
    SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    problemDetailsSchema = registry.getSchema(contract.at("/components/schemas/ProblemDetails"));
  }

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  void theCreatedEventIsReadBackIdenticalThroughItsOwnLocation() throws Exception {
    // Built from PostgreSQL's own now(), not a fixed literal: a fixed sale_start_time would
    // start failing the margin check (ADR 0028) once that date is in the past.
    Instant saleStart = now().plus(Duration.ofDays(1));
    Instant eventDate = saleStart.plus(Duration.ofDays(30));
    String body = createEventRequestBody(saleStart, eventDate);

    MvcResult created =
        mockMvc
            .perform(
                post("/v1/events")
                    .with(adminAuth())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();

    String location = created.getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    JsonNode createdBody = new ObjectMapper().readTree(created.getResponse().getContentAsString());

    mockMvc
        .perform(get(location).with(userAuth()))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              JsonNode readBody =
                  new ObjectMapper().readTree(result.getResponse().getContentAsString());
              assertThat(readBody.path("id").asString())
                  .isEqualTo(createdBody.path("id").asString());
              assertThat(readBody.path("created_at").asString())
                  .isEqualTo(createdBody.path("created_at").asString());
            });
  }

  @Test
  void theCreatedResponseShowsTheStoredSaleStartTimeNotTheRawRequestValue() throws Exception {
    // PostgreSQL's timestamptz only keeps microsecond precision: a value with finer-grained
    // nanoseconds must come back truncated, and both the 201 and the following GET must show
    // the same, actually stored value (EventRepository.insert now RETURNINGs every column,
    // instead of echoing the method's own Instant parameter).
    Instant saleStart = now().plus(Duration.ofDays(1)).plusNanos(123_456_789);
    Instant eventDate = saleStart.plus(Duration.ofDays(30));
    String body = createEventRequestBody(saleStart, eventDate);

    MvcResult created =
        mockMvc
            .perform(
                post("/v1/events")
                    .with(adminAuth())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode createdBody = new ObjectMapper().readTree(created.getResponse().getContentAsString());
    String storedSaleStartTime = createdBody.path("sale_start_time").asString();
    assertThat(storedSaleStartTime).isNotEqualTo(saleStart.toString());

    String location = created.getResponse().getHeader("Location");
    mockMvc
        .perform(get(location).with(userAuth()))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              JsonNode readBody =
                  new ObjectMapper().readTree(result.getResponse().getContentAsString());
              assertThat(readBody.path("sale_start_time").asString())
                  .isEqualTo(storedSaleStartTime);
            });
  }

  @Test
  void saleStartTimeNotBeforeEventDateIsRejectedAsAValidationProblem() throws Exception {
    Instant saleStart = now().plus(Duration.ofDays(1));
    // Equal to event_date: still a violation of "before", not just "not after".
    String body = createEventRequestBody(saleStart, saleStart);

    mockMvc
        .perform(
            post("/v1/events")
                .with(adminAuth())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(
            result -> {
              String responseBody = result.getResponse().getContentAsString();
              JsonNode node = new ObjectMapper().readTree(responseBody);
              assertThat(node.path("type").asString()).isEqualTo("/problems/validation-error");
              List<Error> errors = problemDetailsSchema.validate(node);
              assertThat(errors).as("Schema violations: %s", errors).isEmpty();
            });
  }

  private Instant now() {
    return jdbcTemplate.queryForObject("SELECT now()", Timestamp.class).toInstant();
  }

  private static String createEventRequestBody(Instant saleStartTime, Instant eventDate) {
    return """
        {
          "name": "Live Concert",
          "capacity": 150,
          "sale_start_time": "%s",
          "event_date": "%s"
        }
        """
        .formatted(saleStartTime, eventDate);
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
    return request -> {
      request.addHeader("Authorization", "Bearer " + tokens().adminToken(Duration.ofHours(1)));
      return request;
    };
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor userAuth() {
    return request -> {
      request.addHeader("Authorization", "Bearer " + tokens().userToken(0, Duration.ofHours(1)));
      return request;
    };
  }
}
