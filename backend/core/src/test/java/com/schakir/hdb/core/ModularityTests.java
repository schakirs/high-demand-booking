package com.schakir.hdb.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  @Test
  void verifiesModuleBoundaries() {
    ApplicationModules modules = ApplicationModules.of(CoreApplication.class);

    assertThatCode(modules::verify).doesNotThrowAnyException();
  }

  @Test
  void detectsTheFourModules() {
    ApplicationModules modules = ApplicationModules.of(CoreApplication.class);

    Set<String> names =
        modules.stream()
            .map(module -> module.getIdentifier().toString())
            .collect(Collectors.toSet());

    assertThat(names).containsExactlyInAnyOrder("event", "user", "inventory", "booking");
  }
}
