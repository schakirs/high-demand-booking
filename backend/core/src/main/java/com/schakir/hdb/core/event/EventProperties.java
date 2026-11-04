package com.schakir.hdb.core.event;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * No default (ADR 0027, ADR 0039): the application must not start without an explicit, strictly
 * positive margin, larger than the event publication registry's resubmission delay.
 * {@code @DurationMin(nanos = 0, inclusive = false)} (Hibernate Validator, already on the classpath
 * via {@code spring-boot-starter-validation}) rejects zero and negative values the same way a
 * missing property does.
 */
@ConfigurationProperties(prefix = "hdb.event")
@Validated
record EventProperties(
    @NotNull @DurationMin(nanos = 0, inclusive = false) Duration saleOpeningMargin) {}
