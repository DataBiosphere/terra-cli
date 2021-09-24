package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.TestCommand;
import harness.baseclasses.ClearContextUnit;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra server` commands. */
@Tag("unit")
public class Server extends ClearContextUnit {
  @Test
  @DisplayName("server status succeeds")
  void serverStatusSucceeds() throws JsonProcessingException {
    // `terra server status`
    String statusMsg =
        TestCommand.runAndParseCommandExpectSuccess(String.class, "server", "status");
    assertEquals("OKAY", statusMsg, "server status returns successfully");
  }

  @Test
  @DisplayName("status, server list reflect server set")
  void statusListReflectSet() throws JsonProcessingException {
    // `terra server set --name=$serverName1`
    String serverName1 = "terra-verily-devel";
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverName1);

    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(serverName1, status.server.name, "status reflects server set");

    // `terra server list`
    // NOTE: there is a JSON output for the terra server list command, but it doesn't flag the
    // current server. the text output puts a "*" next to the current server. that's the reason I'm
    // checking the text output here.
    TestCommand.Result cmd = TestCommand.runCommand("server", "list");
    assertEquals(0, cmd.exitCode, "server list returned successfully");
    // the regex below matches the starred server, which indicates it's the current one (e.g. " *
    // terra-dev")
    String asteriskAtStartOfLine = "(?s).*\\*\\s+";
    assertTrue(
        cmd.stdOut.matches(asteriskAtStartOfLine + serverName1 + ".*"),
        "server list flags correct current server (1)");

    // `terra server set --name=$serverName2`
    String serverName2 = "terra-dev";
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverName2);

    // `terra status`
    status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(serverName2, status.server.name, "status reflects server set");

    // `terra server list`
    cmd = TestCommand.runCommand("server", "list");
    assertEquals(0, cmd.exitCode, "server list returned successfully");
    assertTrue(
        cmd.stdOut.matches(asteriskAtStartOfLine + serverName2 + ".*"),
        "server list flags correct current server (2)");
  }

  @Test
  @DisplayName("server set takes a file path")
  void serverSetTakesFile() throws JsonProcessingException {
    // NOTE: this feature is intended to help debugging without requiring a recompile, not for
    // normal operation
    // `terra server set --name=$filepath`
    Path serverFilePath = TestCommand.getPathForTestInput("BadServer.json");
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverFilePath.toString());

    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals("bad-server", status.server.name, "status reflects server set");

    // `terra server status`
    String statusMsg =
        TestCommand.runAndParseCommandExpectSuccess(String.class, "server", "status");
    assertEquals("ERROR CONNECTING", statusMsg, "server status returns error");

    // `terra server set --name=terra-dev`
    String serverName = "terra-dev";
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverName);

    // `terra status`
    status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(serverName, status.server.name, "status reflects server set");
  }
}
