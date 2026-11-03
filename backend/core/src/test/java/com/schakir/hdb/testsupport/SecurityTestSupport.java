package com.schakir.hdb.testsupport;

import com.schakir.hdb.core.user.testsupport.TestTokens;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for tests that need a real, decodable JWT (D-038): registers one ephemeral ES256 key pair as
 * {@code hdb.security.jwt.public-key-location}, so tokens signed with the same key (via {@link
 * #tokens()}) are accepted by the application's own {@code JwtDecoder} bean — never a hand-built
 * one. The key pair is generated once per JVM, in memory only, never written to disk except for the
 * public half, in a throwaway temp file the decoder reads at startup.
 */
public abstract class SecurityTestSupport {

  private static final TestTokens TOKENS = TestTokens.ephemeral();

  protected static TestTokens tokens() {
    return TOKENS;
  }

  @DynamicPropertySource
  static void registerPublicKey(DynamicPropertyRegistry registry) {
    try {
      Path publicKeyFile = Files.createTempFile("hdb-test-public-key", ".pem");
      Files.writeString(publicKeyFile, TOKENS.publicKeyPem());
      publicKeyFile.toFile().deleteOnExit();
      registry.add(
          "hdb.security.jwt.public-key-location", () -> "file:" + publicKeyFile.toAbsolutePath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
