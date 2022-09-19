package unit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.businessobject.Config.BrowserLaunchOption;
import bio.terra.cli.businessobject.Config.CommandRunnerOption;
import bio.terra.cli.serialization.userfacing.UFLoggingConfig;
import bio.terra.cli.serialization.userfacing.UFServer;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.utils.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.TestCommand;
import harness.TestCommand.Result;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra config` commands. */
@Tag("unit")
public class Config extends SingleWorkspaceUnit {
  @Test
  @DisplayName("app-launch config affects how apps are launched")
  void appLaunch() throws IOException {
    String badImageError = "No such image: badimageid";

    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // set the docker image id to an invalid string
    TestCommand.runCommandExpectSuccess("config", "set", "image", "--image=badimageid");

    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    // apps launched via local process should not be affected
    Result cmd = TestCommand.runCommand("app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");
    // cmd exit code can either be 0=success or 1 because gcloud fails with
    // `(gcloud.config.get-value) Failed to create the default configuration. Ensure your have the
    // correct permissions on`.
    assertThat(
        "Expected to return exit code 0 or 1, instead got " + cmd.exitCode,
        cmd.exitCode == 0 || cmd.exitCode == 1);
    assertThat(
        "bad docker image should not affect local mode",
        cmd.stdErr,
        not(containsString(badImageError)));

    // `terra config set app-launch DOCKER_CONTAINER`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "DOCKER_CONTAINER");

    // apps launched via docker container should error out
    String stdErr =
        TestCommand.runCommandExpectExitCode(3, "app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");
    assertThat("docker image not found error returned", stdErr, containsString(badImageError));
  }

  @Test
  @DisplayName("resource limit config throws error if exceeded")
  void resourceLimit() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // create 2 resources

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name1 = "resourceLimit_1";
    String bucketName1 = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name1, "--bucket-name=" + bucketName1);

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name2 = "resourceLimit_2";
    String bucketName2 = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name2, "--bucket-name=" + bucketName2);

    // set the resource limit to 1
    TestCommand.runCommandExpectSuccess("config", "set", "resource-limit", "--max=1");

    // expect an error when listing the 2 resources created above
    String stdErr = TestCommand.runCommandExpectExitCode(2, "resource", "list");
    assertThat(
        "error thrown when resource limit exceeded",
        stdErr,
        containsString("Total number of resources (2) exceeds the CLI limit (1)"));
  }

  @Test
  @DisplayName("config server set and server set are equivalent")
  void server() throws IOException {
    // It's fine that this test hard-codes server names. We're just testing server configuration is
    // saved correctly; we're not actually making calls to the server.

    // `terra server set --name=verily-devel`
    TestCommand.runCommandExpectSuccess("server", "set", "--name=verily-devel", "--quiet");

    // `terra config get server`
    UFServer getValue =
        TestCommand.runAndParseCommandExpectSuccess(UFServer.class, "config", "get", "server");
    assertEquals("verily-devel", getValue.name, "server set affects config get");

    // `terra config list`
    List<HashMap> configItemList =
        TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        "verily-devel",
        getTableFormatValue(configItemList, "server"),
        "server set affects config list");

    // `terra config set server --name=broad-dev`
    TestCommand.runCommandExpectSuccess("server", "set", "--name=broad-dev", "--quiet");

    // `terra config get server`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(UFServer.class, "config", "get", "server");
    assertEquals("broad-dev", getValue.name, "config set server affects config get");

    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        "broad-dev",
        getTableFormatValue(configItemList, "server"),
        "config set server affects config list");
  }

  @Test
  @DisplayName("config workspace set and workspace set are equivalent")
  void workspace() throws IOException {
    workspaceCreator.login();

    // `terra workspace create`
    UFWorkspace workspace2 = WorkspaceUtils.createWorkspace(workspaceCreator);

    // `terra config get workspace`
    UFWorkspace getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get", "workspace");
    assertEquals(workspace2.id, getValue.id, "workspace create affects config get");

    // `terra config list`
    List<HashMap> configItemList =
        TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        workspace2.id,
        getTableFormatValue(configItemList, "workspace"),
        "workspace create affects config list");

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra config get workspace`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get", "workspace");
    assertEquals(getUserFacingId(), getValue.id, "workspace set affects config get");

    // `terra config list`
    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        getUserFacingId(),
        getTableFormatValue(configItemList, "workspace"),
        "workspace set affects config list");

    // `terra config set workspace --id=$id2`
    TestCommand.runCommandExpectSuccess("config", "set", "workspace", "--id=" + workspace2.id);

    // `terra config get workspace`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get", "workspace");
    assertEquals(workspace2.id, getValue.id, "config set workspace affects config get");

    // `terra config list`
    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        workspace2.id,
        getTableFormatValue(configItemList, "workspace"),
        "confg set workspace affects config list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra config get workspace`
    getValue =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "config", "get", "workspace");
    assertNull(getValue, "workspace delete affects config get");

    // `terra config list`
    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        "",
        getTableFormatValue(configItemList, "workspace"),
        "workspace delete affects config list");
  }

  @Test
  @DisplayName("config get and list always match")
  void getValueList() throws IOException {
    // toggle each config value and make sure config get and list always match.
    // server and workspace config properties are not included here because they are covered by
    // tests above.

    // `terra config set browser MANUAL`
    TestCommand.runCommandExpectSuccess("config", "set", "browser", "MANUAL");
    // `terra config get browser`
    BrowserLaunchOption browser =
        TestCommand.runAndParseCommandExpectSuccess(
            BrowserLaunchOption.class, "config", "get", "browser");
    assertEquals(BrowserLaunchOption.MANUAL, browser, "get reflects set for browser");
    // `terra config list`
    List<HashMap> configItemList =
        TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        BrowserLaunchOption.MANUAL,
        getTableFormatValue(configItemList, "browser"),
        "list reflects set for browser");

    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");
    // `terra config get app-launch`
    CommandRunnerOption appLaunch =
        TestCommand.runAndParseCommandExpectSuccess(
            CommandRunnerOption.class, "config", "get", "app-launch");
    assertEquals(CommandRunnerOption.LOCAL_PROCESS, appLaunch, "get reflects set for app-launch");
    // `terra config list`
    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        CommandRunnerOption.LOCAL_PROCESS,
        getTableFormatValue(configItemList, "app-launch"),
        "list reflects set for app-launch");

    // `terra config set image --image=badimageid123`
    String imageId = "badimageid123";
    TestCommand.runCommandExpectSuccess("config", "set", "image", "--image=" + imageId);
    // `terra config get image`
    String image =
        TestCommand.runAndParseCommandExpectSuccess(String.class, "config", "get", "image");
    assertEquals(imageId, image, "get reflects set for image");
    // `terra config list`
    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        imageId, getTableFormatValue(configItemList, "image"), "list reflects set for image");

    // `terra config set resource-limit --max=3`
    TestCommand.runCommandExpectSuccess("config", "set", "resource-limit", "--max=3");
    // `terra config get resource-limit`
    int resourceLimit =
        TestCommand.runAndParseCommandExpectSuccess(
            Integer.class, "config", "get", "resource-limit");
    assertEquals(3, resourceLimit, "get reflects set for resource-limit");
    // `terra config list`
    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        3,
        getTableFormatValue(configItemList, "resource-limit"),
        "list reflects set for resource-limit");

    // `terra config set logging --console --level=ERROR`
    TestCommand.runCommandExpectSuccess("config", "set", "logging", "--console", "--level=ERROR");
    // `terra config set logging --file --level=TRACE`
    TestCommand.runCommandExpectSuccess("config", "set", "logging", "--file", "--level=TRACE");
    // `terra config get logging`
    UFLoggingConfig logging =
        TestCommand.runAndParseCommandExpectSuccess(
            UFLoggingConfig.class, "config", "get", "logging");
    assertEquals(
        Logger.LogLevel.ERROR, logging.consoleLoggingLevel, "get reflects set for console logging");
    assertEquals(
        Logger.LogLevel.TRACE, logging.fileLoggingLevel, "get reflects set for file logging");
    // `terra config list`
    configItemList = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        Logger.LogLevel.ERROR,
        getTableFormatValue(configItemList, "console-logging"),
        "list reflects set for console logging");
    assertEquals(
        Logger.LogLevel.TRACE,
        getTableFormatValue(configItemList, "file-logging"),
        "list reflects set for file logging");
  }

  @Test
  @DisplayName("config format determines default output format")
  void format() throws JsonProcessingException {
    // Note: we can't use runAndParseCommandExpectSuccess() here, since it sets the format
    // to JSON internally.

    TestCommand.runCommandExpectSuccess("config", "set", "format", "json");
    // if this works, the format was valid json
    List<HashMap> result =
        TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");
    assertEquals(
        "JSON",
        getTableFormatValue(result, "format"),
        "workspace create affects config list in Json format");

    TestCommand.runCommand("config", "set", "format", "text");
    Result result2 = TestCommand.runCommand("config", "list");

    // assert header is correct
    String[] rows = result2.stdOut.split("\\n");
    String[] rowHead = rows[0].split("\\s+");
    assertEquals("OPTION", rowHead[0]);
    assertEquals("VALUE", rowHead[1]);
    assertEquals("DESCRIPTION", rowHead[2]);

    // assert email and policies are correct
    ArrayList<String> expectedOptions =
        new ArrayList<>(
            List.of(
                "OPTION",
                "app-launch",
                "browser",
                "image",
                "resource-limit",
                "console-logging",
                "file-logging",
                "server",
                "workspace",
                "format"));

    for (int i = 0; i < rows.length; i++) {
      assertEquals(rows[i].split("\\s+")[0], expectedOptions.get(i));
    }

    // --format switch overrides current setting
    result = TestCommand.runAndParseCommandExpectSuccess(ArrayList.class, "config", "list");

    assertEquals(
        "TEXT",
        getTableFormatValue(result, "format"),
        "workspace create affects config list in Json format");
  }

  private String getTableFormatValue(List<HashMap> result, String Option) {
    return result.stream()
        .filter(x -> (x.get("option").equals(Option)))
        .findFirst()
        .orElse(new HashMap<>(Map.of("value", "")))
        .get("value")
        .toString();
  }
}
