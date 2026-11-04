package com.schakir.hdb.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/**
 * {@link EventProperties} must not let the application start without an explicit, strictly positive
 * {@code hdb.event.sale-opening-margin} (ADR 0027, ADR 0039). A minimal {@link
 * ApplicationContextRunner} context, not a full {@code @SpringBootTest} with Testcontainers: no
 * database or web layer is involved in this rule.
 */
class EventPropertiesStartupTests {

  @EnableConfigurationProperties(EventProperties.class)
  static class EnableEventProperties {}

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
          .withUserConfiguration(EnableEventProperties.class);

  @Test
  void startsWithAnExplicitPositiveMargin() {
    contextRunner
        .withPropertyValues("hdb.event.sale-opening-margin=PT1H")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void failsToStartWithoutAMargin() {
    contextRunner.run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT0S", "-PT1S"})
  void failsToStartWithAZeroOrNegativeMargin(String margin) {
    contextRunner
        .withPropertyValues("hdb.event.sale-opening-margin=" + margin)
        .run(context -> assertThat(context).hasFailed());
  }
}
