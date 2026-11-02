package com.schakir.hdb.core.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.ObjectMapper;

/** Proves the global Jackson configuration (D-002, D-025): snake_case, ISO 8601 UTC. */
@JsonTest
class JacksonConfigurationTests {

  @Autowired private ObjectMapper objectMapper;

  record Sample(String multiWordField, Instant someInstant) {}

  @Test
  void serializesPropertiesInSnakeCase() throws Exception {
    String json =
        objectMapper.writeValueAsString(new Sample("value", Instant.parse("2026-01-01T00:00:00Z")));

    assertThat(json).contains("\"multi_word_field\":\"value\"");
  }

  @Test
  void serializesInstantAsIso8601String() throws Exception {
    String json =
        objectMapper.writeValueAsString(new Sample("value", Instant.parse("2026-01-01T00:00:00Z")));

    assertThat(json).contains("\"some_instant\":\"2026-01-01T00:00:00Z\"");
  }
}
