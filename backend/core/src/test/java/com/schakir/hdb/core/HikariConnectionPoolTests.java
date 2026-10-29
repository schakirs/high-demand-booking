package com.schakir.hdb.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class HikariConnectionPoolTests {

  @Autowired private DataSource dataSource;

  @Test
  void poolIsConfiguredExplicitly() {
    assertThat(dataSource).isInstanceOf(HikariDataSource.class);

    HikariDataSource hikari = (HikariDataSource) dataSource;
    assertThat(hikari.getMaximumPoolSize()).isEqualTo(10);
    assertThat(hikari.getMinimumIdle()).isEqualTo(10);
    assertThat(hikari.getConnectionTimeout()).isEqualTo(250);
  }

  @Test
  void poolFailsFastWhenExhausted() throws SQLException {
    List<Connection> borrowed = new ArrayList<>();
    try {
      for (int i = 0; i < 10; i++) {
        borrowed.add(dataSource.getConnection());
      }

      Instant start = Instant.now();
      assertThatThrownBy(dataSource::getConnection)
          .isInstanceOf(SQLTransientConnectionException.class);
      Duration elapsed = Duration.between(start, Instant.now());

      // It waited for roughly the configured 250ms timeout...
      assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(200));
      // ...and did not fall back to the 30s default.
      assertThat(elapsed).isLessThan(Duration.ofMillis(1000));
    } finally {
      for (Connection connection : borrowed) {
        connection.close();
      }
    }
  }
}
