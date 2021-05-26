package harness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hamcrest.CoreMatchers;

/**
 * This class provides utility methods for launching local child processes. This class is intended
 * for integration tests because it calls the commands from a bash script in a separate process.
 */
public class TestBashScript {
  /**
   * Executes a test script in a separate process from the current working directory.
   *
   * @param scriptName name of the script in the test/resources/testscripts directory (e.g.
   *     NextflowRnaseq.sh)
   * @return process exit code
   */
  public static int runScript(String scriptName) {
    // build the command from the script name
    Path script = TestBashScript.getPathFromScriptName(scriptName);
    List<String> command = Collections.singletonList("bash " + script);

    return runCommands(command, Collections.emptyMap());
  }

  /**
   * Executes a command in a separate process from the given working directory, with the given
   * environment variables set beforehand. Adds `terra` to the $PATH.
   *
   * @param command the command and arguments to execute
   * @param envVars the environment variables to set or overwrite if already defined
   * @return process exit code
   */
  public static int runCommands(List<String> command, Map<String, String> envVars) {
    // execute the commands via bash
    List<String> bashCommand = new ArrayList<>();
    bashCommand.add("bash");
    bashCommand.add("-cx"); // -x option = print out the commands as they run
    bashCommand.add(String.join("; ", command));

    // add to the $PATH the directory where the CLI is installed
    String installLocation = System.getProperty("TERRA_INSTALL_DIR");
    assertThat(
        "terra install directory is defined",
        installLocation,
        CoreMatchers.not(emptyOrNullString()));
    envVars.put("PATH", installLocation + ":" + System.getenv("PATH"));

    // use a working directory inside the gradle build directory, so it gets cleaned up with the
    // clean task
    Path workingDirectory = Path.of(System.getProperty("TERRA_WORKING_DIR"));

    // run the commands in a child process
    return launchChildProcess(bashCommand, envVars, workingDirectory);
  }

  /**
   * Executes a command in a separate process from the given working directory, with the given
   * environment variables set beforehand. Blocks until the process exits. Standard out/err are
   * redirected to those of the parent process.
   *
   * @param command the command and arguments to execute
   * @param envVars the environment variables to set or overwrite if already defined
   * @param workingDirectory the working directory to launch the process from
   * @return process exit code
   */
  public static int launchChildProcess(
      List<String> command, Map<String, String> envVars, Path workingDirectory) {
    // build and run process from the specified working directory
    ProcessBuilder procBuilder = new ProcessBuilder(command);
    if (workingDirectory != null) {
      procBuilder.directory(workingDirectory.toFile());
    }
    if (envVars != null) {
      Map<String, String> procEnvVars = procBuilder.environment();
      envVars.entrySet().forEach(envVar -> procEnvVars.put(envVar.getKey(), envVar.getValue()));
    }
    procBuilder.inheritIO();

    // kick off the child process
    Process process;
    try {
      process = procBuilder.start();
    } catch (IOException ioEx) {
      throw new RuntimeException("Error launching child process", ioEx);
    }

    // block until the child process terminates
    try {
      return process.waitFor();
    } catch (InterruptedException intEx) {
      throw new RuntimeException("Error waiting for child process to terminate", intEx);
    }
  }

  /**
   * Helper method to get the absolute path to a script in the test/resources/testscripts directory.
   */
  public static Path getPathFromScriptName(String name) {
    Path scriptPath =
        Path.of(TestBashScript.class.getClassLoader().getResource("testscripts/" + name).getPath())
            .toAbsolutePath();
    return scriptPath;
  }
}
