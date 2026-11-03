package com.schakir.hdb.core.user.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The CLI entry point (D-030, D-038): key reuse, rotation, and deterministic user ids. */
class TestTokensCliTests {

  @Test
  void deterministicUserIdsAreStableAcrossCalls() {
    assertThat(TestTokens.userId(0)).isEqualTo(TestTokens.userId(0));
    assertThat(TestTokens.userId(0))
        .isEqualTo(UUID.nameUUIDFromBytes("user-0".getBytes(StandardCharsets.UTF_8)));
    assertThat(TestTokens.userId(0)).isNotEqualTo(TestTokens.userId(1));
  }

  @Test
  void cliReusesTheSameKeyPairAcrossRuns(@TempDir Path tempDir) throws Exception {
    Path outputDir = tempDir.resolve("identity");
    Path tokensJson = tempDir.resolve("tokens.json");

    runCli(outputDir, tokensJson, false);
    String firstPublicKey = Files.readString(outputDir.resolve("public.pem"));

    runCli(outputDir, tokensJson, false);
    String secondPublicKey = Files.readString(outputDir.resolve("public.pem"));

    assertThat(secondPublicKey).isEqualTo(firstPublicKey);
  }

  @Test
  void privateKeyFileIsRestrictedToOwner(@TempDir Path tempDir) throws Exception {
    assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    Path outputDir = tempDir.resolve("identity");
    Path tokensJson = tempDir.resolve("tokens.json");

    runCli(outputDir, tokensJson, false);

    Set<PosixFilePermission> permissions =
        Files.getPosixFilePermissions(outputDir.resolve("private.pem"));
    assertThat(permissions).isEqualTo(PosixFilePermissions.fromString("rw-------"));
  }

  @Test
  void cliRotateCreatesANewKeyPair(@TempDir Path tempDir) throws Exception {
    Path outputDir = tempDir.resolve("identity");
    Path tokensJson = tempDir.resolve("tokens.json");

    runCli(outputDir, tokensJson, false);
    String firstPublicKey = Files.readString(outputDir.resolve("public.pem"));

    runCli(outputDir, tokensJson, true);
    String secondPublicKey = Files.readString(outputDir.resolve("public.pem"));

    assertThat(secondPublicKey).isNotEqualTo(firstPublicKey);
  }

  private static void runCli(Path outputDir, Path tokensJson, boolean rotate) throws Exception {
    System.setProperty("tokens.users", "2");
    System.setProperty("tokens.ttl", "PT1H");
    System.setProperty("tokens.outputDir", outputDir.toString());
    System.setProperty("tokens.output", tokensJson.toString());
    System.setProperty("tokens.rotate", Boolean.toString(rotate));
    try {
      TestTokens.main(new String[0]);
    } finally {
      System.clearProperty("tokens.users");
      System.clearProperty("tokens.ttl");
      System.clearProperty("tokens.outputDir");
      System.clearProperty("tokens.output");
      System.clearProperty("tokens.rotate");
    }
  }
}
