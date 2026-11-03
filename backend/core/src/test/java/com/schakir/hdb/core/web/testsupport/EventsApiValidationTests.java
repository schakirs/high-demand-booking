package com.schakir.hdb.core.web.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schakir.hdb.testsupport.SecurityTestSupport;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Drives {@code TechnicalProblemHandler} through the real generated {@code EventsApi}
 * (bean-validation annotations the generator emits, not a hand-written {@code @Valid}) via {@link
 * StubEventsApiController}. {@code SampleValidatedController} covers the handler's own logic
 * (unknown properties, non-UUID paths, the 500 fallback); this class only has to prove the
 * generated contract wiring produces the same shape.
 *
 * <p>Extends {@link SecurityTestSupport} only so the application context has a public key to start
 * with (D-038); {@code MockMvcBuilders.webAppContextSetup(...)} without {@code springSecurity()}
 * never runs the security filter chain, so these requests reach the controller regardless of the
 * {@code SCOPE_admin} rule on {@code POST /v1/events} — security itself is covered by the security
 * tests (#6).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
class EventsApiValidationTests extends SecurityTestSupport {

  @Autowired private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

  private static final String VALID_EVENT_BODY =
      """
      {
        "name": "Live Concert",
        "capacity": 150,
        "sale_start_time": "2026-11-14T10:00:00Z",
        "event_date": "2026-12-15T20:00:00Z"
      }
      """;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void invalidCapacityMatchesTheContractsOwnExample() throws Exception {
    String body = VALID_EVENT_BODY.replace("\"capacity\": 150", "\"capacity\": 0");

    mockMvc
        .perform(post("/v1/events").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(
            result -> {
              JsonNode node =
                  new ObjectMapper().readTree(result.getResponse().getContentAsString());
              assertThat(node.path("type").asString()).isEqualTo("/problems/validation-error");
              assertThat(node.path("detail").asString())
                  .isEqualTo("capacity: must be greater than or equal to 1");
            });
  }

  @Test
  void explicitNullOnAnOptionalNonNullableFieldIsRejected() throws Exception {
    String body =
        """
        {
          "name": "Live Concert",
          "capacity": 150,
          "sale_start_time": "2026-11-14T10:00:00Z",
          "event_date": "2026-12-15T20:00:00Z",
          "hold_duration_seconds": null
        }
        """;

    mockMvc
        .perform(post("/v1/events").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(
            result ->
                assertThat(
                        new ObjectMapper()
                            .readTree(result.getResponse().getContentAsString())
                            .path("type")
                            .asString())
                    .isEqualTo("/problems/validation-error"));
  }

  @Test
  void unknownPropertyIsRejected() throws Exception {
    String body =
        VALID_EVENT_BODY.replace("\"capacity\": 150", "\"capacity\": 150, \"not_in_contract\": 1");

    mockMvc
        .perform(post("/v1/events").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(
            result ->
                assertThat(
                        new ObjectMapper()
                            .readTree(result.getResponse().getContentAsString())
                            .path("type")
                            .asString())
                    .isEqualTo("/problems/validation-error"));
  }
}
