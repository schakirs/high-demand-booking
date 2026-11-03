package com.schakir.hdb.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Fail-fast (D-038): the application must not start without the public-key property. */
class JwtDecoderConfigTests {

  @Test
  void contextFailsToStartWithoutThePublicKeyProperty() {
    new ApplicationContextRunner()
        .withUserConfiguration(JwtDecoderConfig.class)
        .withPropertyValues("hdb.security.jwt.clock-skew=60s")
        .run(context -> assertThat(context).hasFailed());
  }
}
