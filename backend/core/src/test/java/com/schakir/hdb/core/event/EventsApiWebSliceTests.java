package com.schakir.hdb.core.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * The web slice for {@code EventsApiController} (ADR 0036): {@link EventService} is mocked, so
 * these tests prove only the controller's own mapping and error translation (persistence is {@link
 * EventIntegrationTests}' job).
 *
 * <p>{@code addFilters = false}: {@code @WebMvcTest} does not load this project's own {@code
 * SecurityConfig} (a plain, component-scanned {@code @Configuration} bean, outside the slice), but
 * the security starter is still on the classpath, so Spring Boot's own default security
 * auto-configuration would otherwise apply and reject every request. The real chain is covered end
 * to end by {@code SecurityFilterChainTests} (#6); this slice intentionally runs with no filters at
 * all.
 */
@WebMvcTest(EventsApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventsApiWebSliceTests {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private EventService eventService;

  private static Schema problemDetailsSchema;
  private static Schema eventSchema;

  private static final String VALID_BODY =
      """
      {
        "name": "Live Concert",
        "capacity": 150,
        "sale_start_time": "2026-11-14T10:00:00Z",
        "event_date": "2026-12-15T20:00:00Z"
      }
      """;

  @BeforeAll
  static void loadSchemas() {
    JsonNode contract = new YAMLMapper().readTree(new File("../../contracts/openapi.yaml"));
    SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    problemDetailsSchema = registry.getSchema(contract.at("/components/schemas/ProblemDetails"));
    eventSchema = registry.getSchema(contract.at("/components/schemas/Event"));
  }

  @Test
  void createdEventIsValidWithARelativeLocationAndTheGeneratedDefaultHoldDuration()
      throws Exception {
    UUID id = UUID.randomUUID();
    EventRecord record =
        new EventRecord(
            id,
            "Live Concert",
            null,
            150,
            Instant.parse("2026-11-14T10:00:00Z"),
            Instant.parse("2026-12-15T20:00:00Z"),
            Duration.ofSeconds(300),
            Instant.parse("2026-11-04T10:00:00Z"));
    // eq(300): the body above omits hold_duration_seconds, so this is only satisfied if the
    // generated CreateEventRequest's own default (300, decision 7 of #7/#12) actually reached
    // the service — otherwise the stub does not match and Mockito returns null, failing loudly.
    given(eventService.createEvent(eq("Live Concert"), isNull(), eq(150), any(), any(), eq(300)))
        .willReturn(record);

    mockMvc
        .perform(post("/v1/events").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/v1/events/" + id))
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.hold_duration_seconds").value(300))
        .andExpect(result -> assertValid(result.getResponse().getContentAsString(), eventSchema));
  }

  @Test
  void saleStartTimeNotBeforeEventDateIsAValidationProblem() throws Exception {
    given(eventService.createEvent(any(), any(), anyInt(), any(), any(), anyInt()))
        .willThrow(new EventValidationException("sale_start_time", "must be before event_date"));

    mockMvc
        .perform(post("/v1/events").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(
            result ->
                assertType(result.getResponse().getContentAsString(), "/problems/validation-error"))
        .andExpect(
            result -> assertValid(result.getResponse().getContentAsString(), problemDetailsSchema));
  }

  @Test
  void saleOpeningMarginViolationIsAValidationProblem() throws Exception {
    given(eventService.createEvent(any(), any(), anyInt(), any(), any(), anyInt()))
        .willThrow(
            new EventValidationException(
                "sale_start_time",
                "must be later than the current time plus the configured margin"));

    mockMvc
        .perform(post("/v1/events").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(
            result ->
                assertType(result.getResponse().getContentAsString(), "/problems/validation-error"))
        .andExpect(
            result -> assertValid(result.getResponse().getContentAsString(), problemDetailsSchema));
  }

  @Test
  void getEventReturnsTheEventBody() throws Exception {
    UUID id = UUID.randomUUID();
    EventRecord record =
        new EventRecord(
            id,
            "Live Concert",
            "An unforgettable concert.",
            150,
            Instant.parse("2026-11-14T10:00:00Z"),
            Instant.parse("2026-12-15T20:00:00Z"),
            Duration.ofSeconds(300),
            Instant.parse("2026-11-04T10:00:00Z"));
    given(eventService.getEvent(id)).willReturn(record);

    mockMvc
        .perform(get("/v1/events/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(result -> assertValid(result.getResponse().getContentAsString(), eventSchema));
  }

  @Test
  void getEventUnknownIdIsAnEventNotFoundProblem() throws Exception {
    UUID id = UUID.randomUUID();
    given(eventService.getEvent(id)).willThrow(new EventNotFoundException(id));

    mockMvc
        .perform(get("/v1/events/" + id))
        .andExpect(status().isNotFound())
        .andExpect(
            result ->
                assertType(result.getResponse().getContentAsString(), "/problems/event-not-found"))
        .andExpect(
            result -> assertValid(result.getResponse().getContentAsString(), problemDetailsSchema));
  }

  private static void assertType(String body, String expected) {
    JsonNode node = new ObjectMapper().readTree(body);
    Assertions.assertThat(node.path("type").asString()).isEqualTo(expected);
  }

  private static void assertValid(String body, Schema schema) {
    JsonNode node = new ObjectMapper().readTree(body);
    List<Error> errors = schema.validate(node);
    Assertions.assertThat(errors).as("Schema violations: %s", errors).isEmpty();
  }
}
