/**
 * The user module: owns token validation and the security filter chain (D-038). Depends on {@code
 * web} to build its 401/403 problem responses. Nothing depends on {@code user}; nothing outside
 * this package needs to be public (Spring finds its beans by package, not by API).
 */
@ApplicationModule(allowedDependencies = "web")
package com.schakir.hdb.core.user;

import org.springframework.modulith.ApplicationModule;
