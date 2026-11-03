package com.schakir.hdb.core.user.testsupport;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates ES256 (P-256) test tokens (D-030, D-038). Two uses:
 *
 * <ul>
 *   <li>Called directly from tests ({@link #ephemeral()}), with a key pair generated in memory,
 *       never written to disk.
 *   <li>Run from the command line (its {@code main} method, via {@code exec-maven-plugin}), where
 *       the key pair is persisted under an output directory and reused across runs unless rotated.
 * </ul>
 *
 * Lives under {@code src/test}: never add token issuance or a private key to {@code src/main}.
 */
public final class TestTokens {

  public static final String ISSUER = "hdb-token-script";
  public static final String AUDIENCE = "hdb-core";
  public static final UUID ADMIN_ID =
      UUID.nameUUIDFromBytes("admin".getBytes(StandardCharsets.UTF_8));

  private final ECKey signingKey;

  private TestTokens(ECKey signingKey) {
    this.signingKey = signingKey;
  }

  /** A key pair generated in memory for this instance only; never written to disk. */
  public static TestTokens ephemeral() {
    try {
      return new TestTokens(new ECKeyGenerator(Curve.P_256).generate());
    } catch (com.nimbusds.jose.JOSEException e) {
      throw new IllegalStateException("Could not generate an EC key pair", e);
    }
  }

  /** Reconstructs an instance from a previously persisted key pair (CLI reuse). */
  static TestTokens fromKeyPair(ECPublicKey publicKey, ECPrivateKey privateKey) {
    ECKey key =
        new ECKey.Builder(Curve.P_256, publicKey)
            .privateKey(privateKey)
            .algorithm(JWSAlgorithm.ES256)
            .build();
    return new TestTokens(key);
  }

  /** Deterministic so a k6 run can be replayed (D-030): {@code UUID.nameUUIDFromBytes}. */
  public static UUID userId(int index) {
    return UUID.nameUUIDFromBytes(("user-" + index).getBytes(StandardCharsets.UTF_8));
  }

  /** The full EC key (including the private key): hand it to tests that need to sign by hand. */
  public ECKey signingKey() {
    return signingKey;
  }

  public String adminToken(Duration ttl) {
    return token(ADMIN_ID, Instant.now(), ttl, Set.of("admin"));
  }

  public String userToken(int index, Duration ttl) {
    return token(userId(index), Instant.now(), ttl, Set.of());
  }

  /** The general case, used directly by the "exp in the past" and TTL tests. */
  public String token(UUID subject, Instant issuedAt, Duration ttl, Set<String> scopes) {
    JWTClaimsSet.Builder claims =
        new JWTClaimsSet.Builder()
            .subject(subject.toString())
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(issuedAt.plus(ttl)));
    if (!scopes.isEmpty()) {
      claims.claim("scope", String.join(" ", scopes));
    }
    return sign(claims.build(), signingKey);
  }

  private static String sign(JWTClaimsSet claims, ECKey key) {
    try {
      SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), claims);
      jwt.sign(new ECDSASigner(key));
      return jwt.serialize();
    } catch (com.nimbusds.jose.JOSEException e) {
      throw new IllegalStateException("Could not sign the token", e);
    }
  }

  public String publicKeyPem() {
    return pem("PUBLIC KEY", publicKeyBytes(signingKey));
  }

  public String privateKeyPem() {
    return pem("PRIVATE KEY", privateKeyBytes(signingKey));
  }

  private static byte[] publicKeyBytes(ECKey key) {
    try {
      return key.toECPublicKey().getEncoded();
    } catch (com.nimbusds.jose.JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  private static byte[] privateKeyBytes(ECKey key) {
    try {
      return key.toECPrivateKey().getEncoded();
    } catch (com.nimbusds.jose.JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String pem(String label, byte[] der) {
    String base64 =
        Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der);
    return "-----BEGIN " + label + "-----\n" + base64 + "\n-----END " + label + "-----\n";
  }

  private static ECPublicKey readPublicKeyPem(String pem) throws GeneralSecurityException {
    byte[] der = decodePem(pem);
    KeyFactory keyFactory = KeyFactory.getInstance("EC");
    return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(der));
  }

  private static ECPrivateKey readPrivateKeyPem(String pem) throws GeneralSecurityException {
    byte[] der = decodePem(pem);
    KeyFactory keyFactory = KeyFactory.getInstance("EC");
    return (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
  }

  private static byte[] decodePem(String pem) {
    String base64 =
        pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
            .replaceAll("-----END [A-Z ]+-----", "")
            .replaceAll("\\s", "");
    return Base64.getDecoder().decode(base64);
  }

  /**
   * CLI entry point (never bound to a build phase; see backend/core/pom.xml). Run with {@code
   * ./mvnw -pl core test-compile exec:java@tokens -Dtokens.users=1000 -Dtokens.ttl=PT1H} from
   * {@code backend/} (CLAUDE.md): {@code exec:java} runs in Maven's own process, so the default
   * output paths are relative to {@code backend/}, not to the {@code core} module's basedir.
   */
  public static void main(String[] args) throws IOException, GeneralSecurityException {
    int userCount = Integer.getInteger("tokens.users", 10);
    Duration ttl = Duration.parse(System.getProperty("tokens.ttl", "PT1H"));
    Path outputDir = Path.of(System.getProperty("tokens.outputDir", "../.local/identity"));
    Path tokensJson = Path.of(System.getProperty("tokens.output", "../load-tests/tokens.json"));
    boolean rotate = Boolean.getBoolean("tokens.rotate");

    TestTokens tokens = loadOrCreateKeyPair(outputDir, rotate);
    writeTokensJson(tokens, userCount, ttl, tokensJson);

    Path publicKeyPath = outputDir.resolve("public.pem").toAbsolutePath().normalize();
    System.out.println("Wrote " + tokensJson.toAbsolutePath().normalize());
    System.out.println("Start the app with:");
    System.out.println("  HDB_SECURITY_JWT_PUBLIC_KEY_LOCATION=file:" + publicKeyPath);
  }

  private static TestTokens loadOrCreateKeyPair(Path outputDir, boolean rotate)
      throws IOException, GeneralSecurityException {
    Files.createDirectories(outputDir);
    Path privatePem = outputDir.resolve("private.pem");
    Path publicPem = outputDir.resolve("public.pem");

    if (!rotate && Files.exists(privatePem) && Files.exists(publicPem)) {
      ECPublicKey publicKey = readPublicKeyPem(Files.readString(publicPem));
      ECPrivateKey privateKey = readPrivateKeyPem(Files.readString(privatePem));
      return fromKeyPair(publicKey, privateKey);
    }

    TestTokens tokens = ephemeral();
    Files.writeString(publicPem, tokens.publicKeyPem());
    Files.writeString(privatePem, tokens.privateKeyPem());
    return tokens;
  }

  private static void writeTokensJson(
      TestTokens tokens, int userCount, Duration ttl, Path tokensJson) throws IOException {
    List<Map<String, String>> users = new ArrayList<>();
    for (int i = 0; i < userCount; i++) {
      users.add(Map.of("id", userId(i).toString(), "token", tokens.userToken(i, ttl)));
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("admin", tokens.adminToken(ttl));
    body.put("users", users);

    Files.createDirectories(tokensJson.getParent());
    JsonMapper mapper = JsonMapper.builder().build();
    Files.writeString(tokensJson, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
  }
}
