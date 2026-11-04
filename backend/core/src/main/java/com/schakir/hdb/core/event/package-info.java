/**
 * The event module: creates and reads events (#7, #12). Depends on {@code web} to build its problem
 * responses (ADR 0037). {@code inventory} is added in #8.
 */
@ApplicationModule(allowedDependencies = "web")
package com.schakir.hdb.core.event;

import org.springframework.modulith.ApplicationModule;
