package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import com.google.common.collect.ImmutableList;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@Tag("unit")
public class SshKeyPair extends SingleWorkspaceUnit {

  private TestUser testUser = TestUser.chooseTestUser();
  private TestUser testUser2 = TestUser.chooseTestUserWhoIsNot(testUser);

  @Test
  // TODO(PF-1497): Remove this once ECM is deployed to all environment in verily.
  @DisabledIfEnvironmentVariable(named = "TERRA_SERVER", matches = "(verily-preprod|verily)")
  void getSshKey() throws IOException {
    testUser.login();
    var sshKeyPair =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");

    assertNotNull(sshKeyPair.privateSshKey);
    assertNotNull(sshKeyPair.publicSshKey);
    assertEquals(testUser.email.toLowerCase(), sshKeyPair.userEmail.toLowerCase());

    var sshKeyPair2 =
        TestCommand.runAndParseCommandExpectSuccess(UFSshKeyPair.class, "user", "ssh-key", "get");

    assertEquals(sshKeyPair.privateSshKey, sshKeyPair2.privateSshKey);
    assertEquals(sshKeyPair.publicSshKey, sshKeyPair2.publicSshKey);
    assertEquals(testUser.email.toLowerCase(), sshKeyPair2.userEmail.toLowerCase());
  }

  @Test
  // TODO(PF-1497): Remove this once ECM is deployed to all environment in verily.
  @DisabledIfEnvironmentVariable(named = "TERRA_SERVER", matches = "(verily-preprod|verily)")
  void generateSshKey() throws IOException {
    testUser.login();
    var sshkey =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");

    var sshkey2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate", "--quiet");

    assertNotEquals(sshkey.privateSshKey, sshkey2.privateSshKey);
    assertNotEquals(sshkey.publicSshKey, sshkey2.publicSshKey);
    assertNotNull(sshkey2.publicSshKey);
    assertNotNull(sshkey2.privateSshKey);
  }

  @Test
  // TODO(PF-1497): Remove this once ECM is deployed to all environment in verily.
  @DisabledIfEnvironmentVariable(named = "TERRA_SERVER", matches = "(verily-preprod|verily)")
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
}
