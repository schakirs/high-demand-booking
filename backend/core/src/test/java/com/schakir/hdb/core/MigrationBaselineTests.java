package com.schakir.hdb.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class MigrationBaselineTests {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void eventAndInventorySchemasExist() {
    List<String> schemas =
        jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata WHERE schema_name IN (?, ?)",
            String.class,
            "event",
            "inventory");

    assertThat(schemas).containsExactlyInAnyOrder("event", "inventory");
  }

  @Test
  void eventModuleMigrationSucceeded() {
    Boolean success =
        jdbcTemplate.queryForObject(
            "SELECT success FROM public.flyway_schema_history_event WHERE version = '1'",
            Boolean.class);

    assertThat(success).isTrue();
  }

  @Test
  void inventoryModuleMigrationSucceeded() {
    Boolean success =
        jdbcTemplate.queryForObject(
            "SELECT success FROM public.flyway_schema_history_inventory WHERE version = '1'",
            Boolean.class);

    assertThat(success).isTrue();
  }
}
