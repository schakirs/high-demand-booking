package com.schakir.hdb.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schakir.hdb.testsupport.SecurityTestSupport;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * End to end: the event created by {@code POST /v1/events} is read back identical by {@code GET
 * /v1/events/{id}} (same {@code created_at}), and the {@code Location} of the 201 resolves to it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
class EventRoundTripHttpTests extends SecurityTestSupport {

  @Autowired private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  void theCreatedEventIsReadBackIdenticalThroughItsOwnLocation() throws Exception {
    String body =
        """
        {
          "name": "Live Concert",
          "capacity": 150,
          "sale_start_time": "2026-11-14T10:00:00Z",
          "event_date": "2026-12-15T20:00:00Z"
        }
        """;

    MvcResult created =
        mockMvc
            .perform(
                post("/v1/events")
                    .with(
                        request -> {
                          request.addHeader(
                              "Authorization",
                              "Bearer " + tokens().adminToken(Duration.ofHours(1)));
                          return request;
                        })
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();

    String location = created.getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    JsonNode createdBody = new ObjectMapper().readTree(created.getResponse().getContentAsString());

    mockMvc
        .perform(
            get(location)
                .with(
                    request -> {
                      request.addHeader(
                          "Authorization", "Bearer " + tokens().userToken(0, Duration.ofHours(1)));
                      return request;
                    }))
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
}
