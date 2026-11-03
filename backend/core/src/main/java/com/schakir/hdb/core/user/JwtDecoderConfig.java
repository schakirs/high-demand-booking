package com.schakir.hdb.core.user;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the ES256 {@link JwtDecoder} by hand (D-038): Boot's own public-key-location
 * auto-configuration only accepts an RSA key ({@code NimbusJwtDecoder.withPublicKey} requires an
 * {@code RSAPublicKey}), and tokens here are signed with an EC (P-256) key. Pinning the key
 * selector to {@link JWSAlgorithm#ES256} is also what rejects every other {@code alg}, including
 * {@code none}: Nimbus's selector only matches a JWS whose header algorithm is exactly ES256.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
class JwtDecoderConfig {

  static final String ISSUER = "hdb-token-script";
  static final String AUDIENCE = "hdb-core";

  @Bean
  JwtDecoder jwtDecoder(JwtProperties properties) throws IOException, GeneralSecurityException {
    Objects.requireNonNull(
        properties.publicKeyLocation(), "hdb.security.jwt.public-key-location must be set");
    Objects.requireNonNull(properties.clockSkew(), "hdb.security.jwt.clock-skew must be set");

    ECPublicKey publicKey = readEcPublicKey(properties.publicKeyLocation());
    ECKey jwk = new ECKey.Builder(Curve.P_256, publicKey).algorithm(JWSAlgorithm.ES256).build();
    ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
    JWSKeySelector<SecurityContext> keySelector =
        new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource);
    ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(keySelector);

    NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            List.of(
                expiryRequired(),
                new JwtTimestampValidator(properties.clockSkew()),
                issuerIs(ISSUER),
                audienceContains(AUDIENCE),
                subjectIsUuid())));
    return decoder;
  }

  private static ECPublicKey readEcPublicKey(Resource resource)
      throws IOException, GeneralSecurityException {
    try (InputStream in = resource.getInputStream()) {
      String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      String base64 =
          pem.replace("-----BEGIN PUBLIC KEY-----", "")
              .replace("-----END PUBLIC KEY-----", "")
              .replaceAll("\\s", "");
      byte[] der = Base64.getDecoder().decode(base64);
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(der));
    }
  }

  private static OAuth2TokenValidator<Jwt> expiryRequired() {
    return jwt ->
        jwt.getExpiresAt() != null
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "exp claim is required", null));
  }

  private static OAuth2TokenValidator<Jwt> issuerIs(String expected) {
    return jwt ->
        expected.equals(jwt.getClaimAsString(JwtClaimNames.ISS))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "iss claim is invalid", null));
  }

  private static OAuth2TokenValidator<Jwt> audienceContains(String expected) {
    return jwt -> {
      List<String> audience = jwt.getClaimAsStringList(JwtClaimNames.AUD);
      return audience != null && audience.contains(expected)
          ? OAuth2TokenValidatorResult.success()
          : OAuth2TokenValidatorResult.failure(
              new OAuth2Error("invalid_token", "aud claim is invalid", null));
    };
  }

  private static OAuth2TokenValidator<Jwt> subjectIsUuid() {
    return jwt -> {
      String subject = jwt.getSubject();
      if (subject == null) {
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "sub claim is required", null));
      }
      try {
        UUID.fromString(subject);
        return OAuth2TokenValidatorResult.success();
      } catch (IllegalArgumentException e) {
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "sub claim must be a UUID", null));
      }
    };
  }
}
