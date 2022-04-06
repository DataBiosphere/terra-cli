package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.UFSshKey;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SshKey extends ClearContextUnit {

  TestUser testUser = TestUser.chooseTestUser();

  @Test
  void getSshKey() throws IOException {
    testUser.login();

    var sshkey =
        TestCommand.runAndParseCommandExpectSuccess(UFSshKey.class, "user", "sshkey", "get");

    assertNotNull(sshkey.privateSshKey);
    assertNotNull(sshkey.publicSshKey);

    var sshkey2 =
        TestCommand.runAndParseCommandExpectSuccess(UFSshKey.class, "user", "sshkey", "get");

    assertEquals(sshkey.privateSshKey, sshkey2.privateSshKey);
    assertEquals(sshkey.publicSshKey, sshkey2.publicSshKey);
  }

  @Test
  void regenerateSshKey() throws IOException {
    testUser.login();
    var sshkey =
        TestCommand.runAndParseCommandExpectSuccess(UFSshKey.class, "user", "sshkey", "get");

    var sshkey2 =
        TestCommand.runAndParseCommandExpectSuccess(UFSshKey.class, "user", "sshkey", "regenerate");

    assertNotEquals(sshkey.privateSshKey, sshkey2.privateSshKey);
    assertNotEquals(sshkey.publicSshKey, sshkey2.publicSshKey);
    assertNotNull(sshkey2.publicSshKey);
    assertNotNull(sshkey2.privateSshKey);
  }
}
