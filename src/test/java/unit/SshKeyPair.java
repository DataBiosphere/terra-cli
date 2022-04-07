package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SshKeyPair extends SingleWorkspaceUnit {

  TestUser testUser;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    testUser = TestUser.chooseTestUser();
    testUser.login();
  }

  @Test
  void getSshKey() throws IOException {
    testUser.login();

    var sshKeyPair =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate");

    assertNotNull(sshKeyPair.privateSshKey);
    assertNotNull(sshKeyPair.publicSshKey);

    var sshKeyPair2 =
        TestCommand.runAndParseCommandExpectSuccess(UFSshKeyPair.class, "user", "ssh-key", "get");

    assertEquals(sshKeyPair.privateSshKey, sshKeyPair2.privateSshKey);
    assertEquals(sshKeyPair.publicSshKey, sshKeyPair2.publicSshKey);
  }

  @Test
  void generateSshKey() throws IOException {
    testUser.login();
    var sshkey =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate");

    var sshkey2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFSshKeyPair.class, "user", "ssh-key", "generate");

    assertNotEquals(sshkey.privateSshKey, sshkey2.privateSshKey);
    assertNotEquals(sshkey.publicSshKey, sshkey2.publicSshKey);
    assertNotNull(sshkey2.publicSshKey);
    assertNotNull(sshkey2.privateSshKey);
  }
}
