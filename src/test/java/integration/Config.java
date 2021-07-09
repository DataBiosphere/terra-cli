package integration;

import harness.TestBashScript;
import harness.baseclasses.ClearContextIntegration;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("integration123")
public class Config extends ClearContextIntegration {
  @Test
  @DisplayName("browser manual config prompts for code")
  void browserManual() throws IOException {
    String authLoginOutputFilename = "Config_browserManual_stdout.txt";
    List<String> commands = new ArrayList<>();
    commands.add("terra config set browser MANUAL");
    commands.add("terra auth revoke");
    commands.add("terra auth login > " + authLoginOutputFilename);
    int exitCode = TestBashScript.runCommands(commands, Collections.emptyMap());
    // expect an error reading stdin: No line found
    assertEquals(3, exitCode, "script failed with an unexpected error");

    // check that the auth login output includes a prompt to enter a code manually
    String authLoginOutput =
        Files.readString(
            TestBashScript.getOutputFilePath(authLoginOutputFilename), StandardCharsets.UTF_8);
    assertThat(
        "auth login output includes prompt to enter code manually",
        authLoginOutput,
        CoreMatchers.containsString("Please enter code:"));
    assertFalse(true, "adding an intential failure!!!");
  }

  @Test
  @DisplayName("logging console config writes debug output")
  void loggingConsole() throws IOException {
    String serverStatusOutputFilename = "Config_loggingConsoleDebug_stdout.txt";
    List<String> commands = new ArrayList<>();
    commands.add("terra config set logging --console --level=DEBUG");
    commands.add("terra server status > " + serverStatusOutputFilename);
    int exitCode = TestBashScript.runCommands(commands, Collections.emptyMap());
    assertEquals(0, exitCode, "server status executed successfully");

    // check that the server status output includes INFO and DEBUG statements
    String serverStatusOutput =
        Files.readString(
            TestBashScript.getOutputFilePath(serverStatusOutputFilename), StandardCharsets.UTF_8);
    assertThat(
        "server status output includes a DEBUG statement",
        serverStatusOutput,
        CoreMatchers.containsString("[main] DEBUG"));
    assertThat(
        "server status output includes an INFO statement",
        serverStatusOutput,
        CoreMatchers.containsString("[main] INFO"));
  }

  @Test
  @DisplayName("image default includes version")
  void imageDefault() throws IOException {
    String imageOutputFilename = "Config_imageDefault_stdout.txt";
    String versionOutputFilename = "Config_version_stdout.txt";
    List<String> commands = new ArrayList<>();
    commands.add("terra config set image --default");
    commands.add("terra config get-value image > " + imageOutputFilename);
    commands.add("terra version > " + versionOutputFilename);
    int exitCode = TestBashScript.runCommands(commands, Collections.emptyMap());
    assertEquals(0, exitCode, "default image and version commands executed successfully");

    // check that the default image id includes the version string
    String imageDefaultOutput =
        Files.readString(
            TestBashScript.getOutputFilePath(imageOutputFilename), StandardCharsets.UTF_8);
    String versionOutput =
        Files.readString(
            TestBashScript.getOutputFilePath(versionOutputFilename), StandardCharsets.UTF_8);
    assertThat(
        "default image includes the version string",
        imageDefaultOutput,
        CoreMatchers.containsString(versionOutput.trim()));
  }
}
