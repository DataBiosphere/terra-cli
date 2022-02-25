package unit;

import static bio.terra.cli.businessobject.Server.ALL_SERVERS_FILENAME;
import static bio.terra.cli.businessobject.Server.RESOURCE_DIRECTORY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.persisted.PDServer;
import bio.terra.cli.serialization.userfacing.UFAuthStatus;
import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.JacksonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra server` commands. */
@Tag("unit")
public class Server extends SingleWorkspaceUnit {
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
    String serverName1 = "verily-devel";
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverName1, "--quiet");

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
    // broad-dev")
    String asteriskAtStartOfLine = "(?s).*✓\\s+";
    assertTrue(
        cmd.stdOut.matches(asteriskAtStartOfLine + serverName1 + ".*"),
        "server list flags correct current server (1)");

    // `terra server set --name=$serverName2`
    String serverName2 = "broad-dev";
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverName2, "--quiet");

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
    Path serverFilePath = TestCommand.getPathForTestInput("servers/BadServer.json");
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverFilePath, "--quiet");

    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals("bad-server", status.server.name, "status reflects server set");

    // `terra server status`
    String statusMsg =
        TestCommand.runAndParseCommandExpectSuccess(String.class, "server", "status");
    assertEquals("ERROR CONNECTING", statusMsg, "server status returns error");

    // `terra server set --name=broad-dev`
    String serverName = "broad-dev";
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverName, "--quiet");

    // `terra status`
    status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(serverName, status.server.name, "status reflects server set");
  }

  @Test
  @DisplayName("service URLs are optional")
  void serviceUrlsAreOptional() throws JsonProcessingException {
    // NOTE: this feature is intended to help debugging without requiring a recompile, not for
    // normal operation
    // `terra server set --name=$filepath`
    Path serverFilePath = TestCommand.getPathForTestInput("servers/MissingDataRepoURI.json");
    TestCommand.runCommandExpectSuccess("server", "set", "--name=" + serverFilePath, "--quiet");

    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals("missing-data-repo-uri", status.server.name, "status reflects server set");

    // `terra server status`
    String statusMsg =
        TestCommand.runAndParseCommandExpectSuccess(String.class, "server", "status");
    assertEquals("OKAY", statusMsg, "server status returns successfully");
  }

  @Test
  @DisplayName(
      "server set clears the auth and workspace context, doesn't prompt if already cleared")
  void serverSetClearsAuthAndWorkspace() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra server set --name=broad-dev-cli-testing --quiet`
    TestCommand.runCommandExpectSuccess("server", "set", "--name=broad-dev-cli-testing", "--quiet");

    // `terra auth status --format=json`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");
    assertFalse(authStatus.loggedIn, "auth status indicates user is logged out");

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertNull(status.workspace, "status indicates workspace is unset");
    assertEquals("broad-dev-cli-testing", status.server.name, "status indicates server is changed");

    // `terra server set --name=broad-dev`
    TestCommand.runCommandExpectSuccess("server", "set", "--name=broad-dev");
    // now that the auth and workspace context are cleared, we shouldn't need the --quiet flag
    // anymore
  }

  @Test
  @DisplayName("server set aborts if the prompt response is no")
  void serverSetChecksPromptResponse() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra server set --name=broad-dev-cli-testing --quiet`
    ByteArrayInputStream stdIn = new ByteArrayInputStream("NO".getBytes(StandardCharsets.UTF_8));
    TestCommand.Result cmd =
        TestCommand.runCommand(stdIn, "server", "set", "--name=broad-dev-cli-testing");
    assertEquals(1, cmd.exitCode, "server set command threw a user actionable exception");
    assertThat(
        "output message says the server switch was aborted",
        cmd.stdErr,
        CoreMatchers.containsString("Switching server aborted"));
  }

  @Test
  @DisplayName("server names match json file names")
  void serverNamesMatchFileNames() throws IOException {
    // read in the list of servers file
    InputStream inputStream =
        FileUtils.getResourceFileHandle(RESOURCE_DIRECTORY + "/" + ALL_SERVERS_FILENAME);
    List<String> allServerFileNames = JacksonMapper.getMapper().readValue(inputStream, List.class);

    // loop through the file names, reading in from JSON
    for (String serverFileName : allServerFileNames) {
      PDServer pdServer = bio.terra.cli.businessobject.Server.fromJsonFile(serverFileName);

      assertEquals(serverFileName, pdServer.name + ".json", "server name matches file name");
    }
  }
}
