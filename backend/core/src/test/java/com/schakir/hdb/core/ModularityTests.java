package com.schakir.hdb.core;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  @Test
  void verifiesModuleBoundaries() {
    ApplicationModules modules = ApplicationModules.of(CoreApplication.class);

    assertThatCode(modules::verify).doesNotThrowAnyException();
  }
}
