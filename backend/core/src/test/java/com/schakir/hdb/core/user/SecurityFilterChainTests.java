package com.schakir.hdb.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.schakir.hdb.core.user.testsupport.TestTokens;
import com.schakir.hdb.testsupport.SecurityTestSupport;
import com.schakir.hdb.testsupport.TestcontainersConfiguration;
import java.io.File;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * The security filter chain end to end (D-038, CLAUDE.md rule 10): every contract operation through
 * the real application context, the real {@code JwtDecoder}, and {@link
 * StubHoldsApiController}/{@code StubEventsApiController} (temporary scaffolding, #5/#7).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
class SecurityFilterChainTests extends SecurityTestSupport {

  private static final TestTokens OTHER_SIGNER = TestTokens.ephemeral();

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private ApplicationContext applicationContext;
  @Autowired private JwtDecoder jwtDecoder;
  private MockMvc mockMvc;

  private static Schema problemDetailsSchema;

  @BeforeAll
  static void loadProblemDetailsSchema() {
    JsonNode contract = new YAMLMapper().readTree(new File("../../contracts/openapi.yaml"));
    JsonNode schemaNode = contract.at("/components/schemas/ProblemDetails");
    SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    problemDetailsSchema = registry.getSchema(schemaNode);
  }

  @BeforeEach
  void setUp() {
    // Unlike the plain webAppContextSetup used elsewhere in this module (#5), this class tests
    // authorization itself, so the real Spring Security filter chain must actually run.
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  // --- Contract-driven guardrail (CLAUDE.md rule 10): every operation in the contract requires
  // at least authentication. Read from contracts/openapi.yaml, not hand-written. ---

  static Stream<Arguments> contractOperations() {
    JsonNode contract = new YAMLMapper().readTree(new File("../../contracts/openapi.yaml"));
    JsonNode paths = contract.at("/paths");
    List<Arguments> operations = new java.util.ArrayList<>();
    for (String path : paths.propertyNames()) {
      for (String method : paths.get(path).propertyNames()) {
        operations.add(Arguments.of(method, path));
      }
    }
    return operations.stream();
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("contractOperations")
  void everyContractOperationRequiresAuthentication(String method, String path) throws Exception {
    String resolvedPath = path.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());

    mockMvc
        .perform(requestFor(method, resolvedPath))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      requestFor(String method, String path) {
    return switch (method) {
      case "get" -> get(path);
      case "post" -> post(path);
      default -> throw new IllegalStateException("Unsupported contract method: " + method);
    };
  }

  // --- Admin scope (CLAUDE.md rule 10) ---

  @Test
  void createEventWithoutAdminScopeIsForbidden() throws Exception {
    mockMvc
        .perform(post("/v1/events").with(userJwt(0)))
        .andExpect(status().isForbidden())
        .andExpect(header().string("WWW-Authenticate", "Bearer error=\"insufficient_scope\""))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  @Test
  void createEventWithAdminScopePassesSecurity() throws Exception {
    mockMvc
        .perform(post("/v1/events").with(adminJwt()))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void theOtherFourOperationsPassSecurityForAnyAuthenticatedUser() throws Exception {
    UUID id = UUID.randomUUID();
    mockMvc
        .perform(get("/v1/events/" + id).with(userJwt(0)))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    mockMvc
        .perform(post("/v1/events/" + id + "/holds").with(userJwt(0)))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    mockMvc
        .perform(get("/v1/holds/" + id).with(userJwt(0)))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    mockMvc
        .perform(post("/v1/holds/" + id + "/confirmations").with(userJwt(0)))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  // --- Actuator and unknown routes ---

  @Test
  void actuatorHealthAndPrometheusArePublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
  }

  @Test
  void actuatorEnvIsDeniedByDefault() throws Exception {
    mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/actuator/env").with(userJwt(0))).andExpect(status().isForbidden());
  }

  @Test
  void unknownRouteIsDeniedNotNotFound() throws Exception {
    mockMvc.perform(get("/v1/does-not-exist")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/v1/does-not-exist").with(userJwt(0))).andExpect(status().isForbidden());
  }

  // --- RFC 9728 metadata endpoint is not in the contract: must not be reachable (Must-fix 1) ---

  @Test
  void protectedResourceMetadataEndpointIsNotExposed() throws Exception {
    mockMvc
        .perform(get("/.well-known/oauth-protected-resource"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"));
    mockMvc
        .perform(get("/.well-known/oauth-protected-resource").with(userJwt(0)))
        .andExpect(status().isForbidden())
        .andExpect(header().string("WWW-Authenticate", "Bearer error=\"insufficient_scope\""));
  }

  // --- Order: authentication/authorization before the body is read ---

  @Test
  void anonymousWithInvalidBodyIsUnauthorizedNotBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/v1/events")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("not json"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonAdminWithInvalidBodyIsForbiddenNotBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/v1/events")
                .with(userJwt(0))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("not json"))
        .andExpect(status().isForbidden());
  }

  // --- Caller id comes from the token only ---

  @Test
  void callerIdComesFromTheTokenOnly() throws Exception {
    UUID tokenSubject = TestTokens.userId(0);
    UUID otherClaimedId = TestTokens.userId(1);
    UUID holdId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/v1/holds/" + holdId)
                .with(userJwt(0))
                .header("X-User-Id", otherClaimedId.toString())
                .queryParam("user_id", otherClaimedId.toString()))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              JsonNode body =
                  new ObjectMapper().readTree(result.getResponse().getContentAsString());
              assertThat(body.path("id").asString()).isEqualTo(tokenSubject.toString());
            });
  }

  // --- TestTokens produces tokens the real decoder bean accepts, with the TTL honoured ---

  @Test
  void theApplicationsDecoderAcceptsTestTokensAndHonoursTheTtl() {
    Instant issuedAt = Instant.now();
    Duration ttl = Duration.ofHours(2);
    String token = tokens().token(TestTokens.userId(0), issuedAt, ttl, Set.of());

    Jwt jwt = jwtDecoder.decode(token);

    assertThat(jwt.getSubject()).isEqualTo(TestTokens.userId(0).toString());
    assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(ttl));
  }

  // --- Clock skew: 60s configured, exactly as advertised ---

  @Test
  void expiryWithinTheSkewIsAccepted() throws Exception {
    Instant exp = Instant.now().minusSeconds(30);
    String token =
        tokens()
            .token(TestTokens.userId(0), exp.minusSeconds(60), Duration.ofSeconds(60), Set.of());

    mockMvc
        .perform(get("/v1/events/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
  }

  @Test
  void expiryBeyondTheSkewIsRejected() throws Exception {
    Instant exp = Instant.now().minusSeconds(90);
    String token =
        tokens()
            .token(TestTokens.userId(0), exp.minusSeconds(60), Duration.ofSeconds(60), Set.of());

    mockMvc
        .perform(get("/v1/events/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\""));
  }

  // --- No default user / generated password (requirement 6) ---

  @Test
  void noUserDetailsServiceIsConfigured() {
    assertThat(applicationContext.getBeanNamesForType(UserDetailsService.class)).isEmpty();
  }

  // --- Bad tokens: one representative operation, every rejection reason (parameterized) ---

  static Stream<Arguments> badTokenVariants() {
    Instant now = Instant.now();
    Date iat = Date.from(now);
    Date validExp = Date.from(now.plusSeconds(3600));
    String validSub = TestTokens.userId(0).toString();

    return Stream.of(
        Arguments.of(
            "expired beyond skew",
            tokens()
                .token(
                    TestTokens.userId(0), now.minusSeconds(150), Duration.ofSeconds(60), Set.of())),
        Arguments.of(
            "wrong signing key",
            OTHER_SIGNER.token(TestTokens.userId(0), now, Duration.ofHours(1), Set.of())),
        Arguments.of(
            "alg HS256",
            signHs256(
                new JWTClaimsSet.Builder()
                    .subject(validSub)
                    .issuer(TestTokens.ISSUER)
                    .audience(TestTokens.AUDIENCE)
                    .issueTime(iat)
                    .expirationTime(validExp)
                    .build())),
        Arguments.of(
            "alg none",
            new PlainJWT(
                    new JWTClaimsSet.Builder()
                        .subject(validSub)
                        .issuer(TestTokens.ISSUER)
                        .audience(TestTokens.AUDIENCE)
                        .issueTime(iat)
                        .expirationTime(validExp)
                        .build())
                .serialize()),
        Arguments.of(
            "wrong issuer",
            signEs256(
                new JWTClaimsSet.Builder()
                    .subject(validSub)
                    .issuer("someone-else")
                    .audience(TestTokens.AUDIENCE)
                    .issueTime(iat)
                    .expirationTime(validExp)
                    .build())),
        Arguments.of(
            "wrong audience",
            signEs256(
                new JWTClaimsSet.Builder()
                    .subject(validSub)
                    .issuer(TestTokens.ISSUER)
                    .audience("someone-else")
                    .issueTime(iat)
                    .expirationTime(validExp)
                    .build())),
        Arguments.of(
            "missing subject",
            signEs256(
                new JWTClaimsSet.Builder()
                    .issuer(TestTokens.ISSUER)
                    .audience(TestTokens.AUDIENCE)
                    .issueTime(iat)
                    .expirationTime(validExp)
                    .build())),
        Arguments.of(
            "non-UUID subject",
            signEs256(
                new JWTClaimsSet.Builder()
                    .subject("not-a-uuid")
                    .issuer(TestTokens.ISSUER)
                    .audience(TestTokens.AUDIENCE)
                    .issueTime(iat)
                    .expirationTime(validExp)
                    .build())),
        Arguments.of(
            "missing exp",
            signEs256(
                new JWTClaimsSet.Builder()
                    .subject(validSub)
                    .issuer(TestTokens.ISSUER)
                    .audience(TestTokens.AUDIENCE)
                    .issueTime(iat)
                    .build())));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("badTokenVariants")
  void rejectsBadTokens(String caseName, String token) throws Exception {
    mockMvc
        .perform(get("/v1/events/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\""))
        .andExpect(
            result ->
                assertValidAgainstProblemDetailsSchema(result.getResponse().getContentAsString()));
  }

  private static String signEs256(JWTClaimsSet claims) {
    try {
      SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), claims);
      jwt.sign(new ECDSASigner(tokens().signingKey()));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String signHs256(JWTClaimsSet claims) {
    try {
      byte[] secret = new byte[32];
      new SecureRandom().nextBytes(secret);
      SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).build(), claims);
      jwt.sign(new MACSigner(secret));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt(
      int index) {
    String token = tokens().userToken(index, Duration.ofHours(1));
    return request -> {
      request.addHeader("Authorization", "Bearer " + token);
      return request;
    };
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
    String token = tokens().adminToken(Duration.ofHours(1));
    return request -> {
      request.addHeader("Authorization", "Bearer " + token);
      return request;
    };
  }

  private static void assertValidAgainstProblemDetailsSchema(String body) {
    JsonNode node = new ObjectMapper().readTree(body);
    List<Error> errors = problemDetailsSchema.validate(node);
    assertThat(errors).as("ProblemDetails schema violations: %s", errors).isEmpty();
  }
}
