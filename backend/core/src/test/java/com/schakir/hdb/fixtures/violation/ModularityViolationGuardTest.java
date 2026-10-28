package com.schakir.hdb.fixtures.violation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

/**
 * Proves that {@link ApplicationModules#verify()} actually detects a module reaching into another
 * module's internal package: {@code modulea} references {@code moduleb.internal}, which must fail
 * verification. Lives outside {@code com.schakir.hdb.core} so neither the application's component
 * scan nor {@code ModularityTests} ever sees it.
 */
class ModularityViolationGuardTest {

  @Test
  void detectsInternalPackageViolation() {
    // Spring Modulith excludes test-classpath locations by default; this fixture lives under
    // src/test/java on purpose (see class Javadoc), so every location must be included.
    ApplicationModules modules =
        ApplicationModules.of("com.schakir.hdb.fixtures.violation", location -> true);

    assertThatThrownBy(modules::verify)
        .isInstanceOf(Violations.class)
        .hasMessageContaining("moduleb.internal");
  }
}
