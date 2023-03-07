package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SshKeyPair extends SingleWorkspaceUnit {
  private final TestUser testUser = TestUser.chooseTestUser();
  private final TestUser testUser2 = TestUser.chooseTestUserWhoIsNot(testUser);

  @Test
  void getSshKey() throws IOException {
    testUser.login();
    var sshKeyPair =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");

    assertNull(sshKeyPair.privateSshKey);
    assertNotNull(sshKeyPair.publicSshKey);
    assertEquals(testUser.email.toLowerCase(), sshKeyPair.userEmail.toLowerCase());

    var sshKeyPair2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "get", "--include-private-key");

    assertNotNull(sshKeyPair2.privateSshKey);
    assertEquals(sshKeyPair.publicSshKey, sshKeyPair2.publicSshKey);
    assertEquals(testUser.email.toLowerCase(), sshKeyPair2.userEmail.toLowerCase());
  }

  @Test
  void addSshKey() throws IOException {
    testUser.login();
    TestCommand.runCommandExpectSuccess("user", "ssh-key", "generate", "--quiet");

    TestCommand.runCommandExpectSuccess("user", "ssh-key", "add");

    assertSshKeyAdded();
  }

  @Test
  void generateSshKey() throws IOException {
    testUser.login();
    var sshkey =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");

    var sshkey2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");

    assertNull(sshkey.privateSshKey);
    assertNull(sshkey2.privateSshKey);
    assertNotEquals(sshkey.publicSshKey, sshkey2.publicSshKey);
    assertNotNull(sshkey2.publicSshKey);
  }

  @Test
  void generateSshKey_saveToFile() throws IOException {
    testUser.login();

    TestCommand.runCommandExpectSuccess("user", "ssh-key", "generate", "--quiet", "--save-to-file");

    assertSshKeyAdded();
  }

  @Test
  void switchUser() throws IOException {
    testUser.login();
    var sshkey =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");
    assertEquals(testUser.email.toLowerCase(), sshkey.userEmail.toLowerCase());

    testUser2.login();
    var sshkey2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");
    assertEquals(testUser2.email.toLowerCase(), sshkey2.userEmail.toLowerCase());
  }

  private void assertSshKeyAdded() {
    String privateKey = System.getProperty("user.home") + "/.ssh/terra_id_rsa";
    String publicKey = System.getProperty("user.home") + "/.ssh/terra_id_rsa.pub";
    assertTrue(Files.exists(Paths.get(privateKey)));
    assertTrue(Files.exists(Paths.get(publicKey)));

    // clean up
    FileUtils.deleteQuietly(new File(privateKey));
    FileUtils.deleteQuietly(new File(publicKey));
  }
}
