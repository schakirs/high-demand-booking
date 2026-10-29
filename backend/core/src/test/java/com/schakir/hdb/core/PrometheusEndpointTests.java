package com.schakir.hdb.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PrometheusEndpointTests {

  @LocalServerPort private int port;

  @Test
  void prometheusEndpointExposesHikariMetrics() {
    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/prometheus", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("hikaricp_connections_max");
  }
}
