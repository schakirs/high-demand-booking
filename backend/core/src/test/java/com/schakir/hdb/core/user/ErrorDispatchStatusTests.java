package com.schakir.hdb.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.schakir.hdb.testsupport.SecurityTestSupport;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Proves, through a real embedded server, that a container ERROR dispatch is not re-authorized into
 * a 401/403 (CLAUDE.md rule 10, D-038): MockMvc never performs a genuine ERROR-dispatch forward, so
 * a {@code sendError} there stays whatever status it was given regardless of the security rule
 * under test, making that assertion vacuous. {@code StubHoldsApiController#confirmHold} calls
 * {@code response.sendError(500)} directly; only a real server forwards that to {@code /error},
 * where it reaches {@code SecurityConfig}'s {@code
 * dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()} rule. Commenting out that rule turns
 * this test's 500 into a 403 (verified manually; see the commit message).
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ErrorDispatchStatusTests extends SecurityTestSupport {

  @LocalServerPort private int port;

  @Test
  void containerErrorDispatchKeepsItsStatus() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokens().userToken(0, Duration.ofHours(1)));
    RestTemplate restTemplate = new RestTemplate();

    try {
      restTemplate.exchange(
          "http://localhost:" + port + "/v1/holds/" + UUID.randomUUID() + "/confirmations",
          HttpMethod.POST,
          new HttpEntity<>(headers),
          String.class);
      fail("Expected a 500 response");
    } catch (HttpServerErrorException e) {
      assertThat(e.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
