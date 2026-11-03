package com.schakir.hdb.core.user;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * No default for {@code publicKeyLocation} (D-038): the application must not start without it.
 * Boot's own {@code spring.security.oauth2.resourceserver.jwt.public-key-location} is not used
 * here; its auto-configuration only accepts an RSA key, and tokens are signed with ES256.
 */
@ConfigurationProperties(prefix = "hdb.security.jwt")
record JwtProperties(Resource publicKeyLocation, Duration clockSkew) {}
