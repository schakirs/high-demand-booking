package com.schakir.hdb.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared PostgreSQL 18 container for database tests, reused through Spring's test-context cache:
 * every test class importing this must use the same {@code @SpringBootTest} configuration (same
 * webEnvironment, same imports) so they share one cached context and therefore one container,
 * instead of starting one per test class.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer("postgres:18.6");
  }
}
