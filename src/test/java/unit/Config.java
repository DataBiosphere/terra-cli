package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.businessobject.Config.BrowserLaunchOption;
import bio.terra.cli.businessobject.Config.CommandRunnerOption;
import bio.terra.cli.serialization.userfacing.UFConfig;
import bio.terra.cli.serialization.userfacing.UFLoggingConfig;
import bio.terra.cli.serialization.userfacing.UFServer;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.utils.Logger;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra config` commands. */
@Tag("unit")
public class Config extends SingleWorkspaceUnit {
  @Test
  @DisplayName("app-launch config affects how apps are launched")
  void appLaunch() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // set the docker image id to an invalid string
    TestCommand.runCommandExpectSuccess("config", "set", "image", "--image=badimageid");

    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    // apps launched via local process should not be affected
    TestCommand.Result cmd =
        TestCommand.runCommand("app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");
    // cmd exit code can either be 0=success or 127=command not found if gcloud is not installed on
    // this machine
    assertThat(
        "app execute with local process did not throw system exception",
        cmd.exitCode == 0 || cmd.exitCode == 127);

    // `terra config set app-launch DOCKER_CONTAINER`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "DOCKER_CONTAINER");

    // apps launched via docker container should error out
    String stdErr =
        TestCommand.runCommandExpectExitCode(3, "app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");
    assertThat(
        "docker image not found error returned",
        stdErr,
        CoreMatchers.containsString("No such image: badimageid"));
  }

  @Test
  @DisplayName("resource limit config throws error if exceeded")
  void resourceLimit() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // create 2 resources

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name1 = "resourceLimit_1";
    String bucketName1 = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources", "create", "gcs-bucket", "--name=" + name1, "--bucket-name=" + bucketName1);

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name2 = "resourceLimit_2";
    String bucketName2 = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources", "create", "gcs-bucket", "--name=" + name2, "--bucket-name=" + bucketName2);

    // set the resource limit to 1
    TestCommand.runCommandExpectSuccess("config", "set", "resource-limit", "--max=1");

    // expect an error when listing the 2 resources created above
    String stdErr = TestCommand.runCommandExpectExitCode(2, "resources", "list");
    assertThat(
        "error thrown when resource limit exceeded",
        stdErr,
        CoreMatchers.containsString("Total number of resources (2) exceeds the CLI limit (1)"));
  }

  @Test
  @DisplayName("config server set and server set are equivalent")
  void server() throws IOException {
    // `terra server set --name=terra-verily-dev`
    TestCommand.runCommandExpectSuccess("server", "set", "--name=terra-verily-dev");

    // `terra config get-value server`
    UFServer getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFServer.class, "config", "get-value", "server");
    assertEquals("terra-verily-dev", getValue.name, "server set affects config get-value");

    // `terra config list`
    UFConfig config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals("terra-verily-dev", config.serverName, "server set affects config list");

    // `terra config set server --name=terra-dev`
    TestCommand.runCommandExpectSuccess("server", "set", "--name=terra-dev");

    // `terra config get-value server`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFServer.class, "config", "get-value", "server");
    assertEquals("terra-dev", getValue.name, "config set server affects config get-value");

    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals("terra-dev", config.serverName, "config set server affects config list");
  }

  @Test
  @DisplayName("config workspace set and workspace set are equivalent")
  void workspace() throws IOException {
    workspaceCreator.login();

    // `terra workspace create`
    UFWorkspace workspace2 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // `terra config get-value workspace`
    UFWorkspace getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get-value", "workspace");
    assertEquals(workspace2.id, getValue.id, "workspace create affects config get-value");

    // `terra config list`
    UFConfig config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(workspace2.id, config.workspaceId, "workspace create affects config list");

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra config get-value workspace`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get-value", "workspace");
    assertEquals(getWorkspaceId(), getValue.id, "workspace set affects config get-value");

    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(getWorkspaceId(), config.workspaceId, "workspace set affects config list");

    // `terra config set workspace --id=$id2`
    TestCommand.runCommandExpectSuccess("config", "set", "workspace", "--id=" + workspace2.id);

    // `terra config get-value workspace`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get-value", "workspace");
    assertEquals(workspace2.id, getValue.id, "config set workspace affects config get-value");

    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(workspace2.id, config.workspaceId, "confg set workspace affects config list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra config get-value workspace`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get-value", "workspace");
    assertNull(getValue, "workspace delete affects config get-value");

    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertNull(config.workspaceId, "workspace delete affects config list");
  }

  @Test
  @DisplayName("config get-value and list always match")
  void getValueList() throws IOException {
    // toggle each config value and make sure config get-value and list always match.
    // server and workspace config properties are not included here because they are covered by
    // tests above.

    // `terra config set browser MANUAL`
    TestCommand.runCommandExpectSuccess("config", "set", "browser", "MANUAL");
    // `terra config get-value browser`
    BrowserLaunchOption browser =
        TestCommand.runAndParseCommandExpectSuccess(
            BrowserLaunchOption.class, "config", "get-value", "browser");
    assertEquals(BrowserLaunchOption.MANUAL, browser, "get-value reflects set for browser");
    // `terra config list`
    UFConfig config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(
        BrowserLaunchOption.MANUAL, config.browserLaunchOption, "list reflects set for browser");

    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");
    // `terra config get-value app-launch`
    CommandRunnerOption appLaunch =
        TestCommand.runAndParseCommandExpectSuccess(
            CommandRunnerOption.class, "config", "get-value", "app-launch");
    assertEquals(
        CommandRunnerOption.LOCAL_PROCESS, appLaunch, "get-value reflects set for app-launch");
    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(
        CommandRunnerOption.LOCAL_PROCESS,
        config.commandRunnerOption,
        "list reflects set for app-launch");

    // `terra config set image --image=badimageid123`
    String imageId = "badimageid123";
    TestCommand.runCommandExpectSuccess("config", "set", "image", "--image=" + imageId);
    // `terra config get-value image`
    String image =
        TestCommand.runAndParseCommandExpectSuccess(String.class, "config", "get-value", "image");
    assertEquals(imageId, image, "get-value reflects set for image");
    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(imageId, config.dockerImageId, "list reflects set for image");

    // `terra config set resource-limit --max=3`
    TestCommand.runCommandExpectSuccess("config", "set", "resource-limit", "--max=3");
    // `terra config get-value resource-limit`
    int resourceLimit =
        TestCommand.runAndParseCommandExpectSuccess(
            Integer.class, "config", "get-value", "resource-limit");
    assertEquals(3, resourceLimit, "get-value reflects set for resource-limit");
    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(3, config.resourcesCacheSize, "list reflects set for resource-limit");

    // `terra config set logging --console --level=ERROR`
    TestCommand.runCommandExpectSuccess("config", "set", "logging", "--console", "--level=ERROR");
    // `terra config set logging --file --level=TRACE`
    TestCommand.runCommandExpectSuccess("config", "set", "logging", "--file", "--level=TRACE");
    // `terra config get-value logging`
    UFLoggingConfig logging =
        TestCommand.runAndParseCommandExpectSuccess(
            UFLoggingConfig.class, "config", "get-value", "logging");
    assertEquals(
        Logger.LogLevel.ERROR,
        logging.consoleLoggingLevel,
        "get-value reflects set for console logging");
    assertEquals(
        Logger.LogLevel.TRACE, logging.fileLoggingLevel, "get-value reflects set for file logging");
    // `terra config list`
    config = TestCommand.runAndParseCommandExpectSuccess(UFConfig.class, "config", "list");
    assertEquals(
        Logger.LogLevel.ERROR, config.consoleLoggingLevel, "list reflects set for console logging");
    assertEquals(
        Logger.LogLevel.TRACE, config.fileLoggingLevel, "list reflects set for file logging");
  }
}
