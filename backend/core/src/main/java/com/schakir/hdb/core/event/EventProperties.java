package com.schakir.hdb.core.event;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * No default (ADR 0027, ADR 0039): the application must not start without an explicit margin of at
 * least one second. Until a periodic resubmission job exists (ADR 0039), this margin only covers an
 * application restart replaying the event publication registry — it does not protect against a
 * publication stuck while the application keeps running.
 *
 * <p>{@code @DurationMin(seconds = 1)} (Hibernate Validator, already on the classpath via {@code
 * spring-boot-starter-validation}): a sub-second value such as {@code PT0.5S} would otherwise pass
 * {@code @NotNull} and then be truncated to zero seconds by {@link Duration#getSeconds()} in {@code
 * EventRepository}, silently disabling the margin.
 */
@ConfigurationProperties(prefix = "hdb.event")
@Validated
record EventProperties(@NotNull @DurationMin(seconds = 1) Duration saleOpeningMargin) {}
