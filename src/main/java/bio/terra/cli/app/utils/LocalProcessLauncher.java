package bio.terra.cli.app.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.utils.UserIO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** This class provides utility methods for launching local child processes. */
public class LocalProcessLauncher {
  private Process process;

  public LocalProcessLauncher() {}

  /**
   * Helper method to stream the child process' output to the CLI console.
   *
   * @param fromStream stream reading from the child process output
   * @param toStream stream writing to the CLI console
   */
  private static void streamOutput(InputStream fromStream, PrintStream toStream) {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(fromStream, StandardCharsets.UTF_8))) {

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        toStream.println(line);
        toStream.flush();
      }
    } catch (IOException ioEx) {
      throw new SystemException("Error streaming output of child process", ioEx);
    }
  }

  /**
   * Executes a command in a separate process from the current working directory (i.e. the same
   * place as this Java process is running).
   *
   * @param command the command and arguments to execute
   * @param envVars the environment variables to set or overwrite if already defined
   */
  public void launchProcess(List<String> command, Map<String, String> envVars) {
    launchProcess(command, envVars, null);
  }

  /**
   * Executes a command in a separate process from the given working directory, with the given
   * environment variables set beforehand.
   *
   * @param command the command and arguments to execute
   * @param envVars the environment variables to set or overwrite if already defined
   * @param workingDirectory the working directory to launch the process from
   */
  public void launchProcess(
      List<String> command, Map<String, String> envVars, Path workingDirectory) {
    // build and run process from the specified working directory
    ProcessBuilder procBuilder = new ProcessBuilder(command);
    if (workingDirectory != null) {
      procBuilder.directory(workingDirectory.toFile());
    }
    if (envVars != null) {
      Map<String, String> procEnvVars = procBuilder.environment();
      procEnvVars.putAll(envVars);
    }

    try {
      process = procBuilder.start();
    } catch (IOException ioEx) {
      throw new SystemException("Error launching local process", ioEx);
    }
  }

  public Process launchSilentProcess(List<String> command, Map<String, String> envVars, Path workingDirectory) {
    launchProcess(command, null, null);
    waitForTerminate();
    return process;
  }

  /** Stream standard out/err from the child process to the CLI console. */
  public void streamOutputForProcess() {
    // getInputStream() is confusingly named; it returns process stdout (what we want).
    Runnable streamStdOut = () -> streamOutput(process.getInputStream(), UserIO.getOut());
    Thread stdOutThread = new Thread(streamStdOut);
    stdOutThread.start();

    Runnable streamStdErr = () -> streamOutput(process.getErrorStream(), UserIO.getErr());
    Thread stdErrThread = new Thread(streamStdErr);
    stdErrThread.start();
  }

  /** Block until the child process terminates, then return its exit code. */
  public int waitForTerminate() {
    try {
      return process.waitFor();
    } catch (InterruptedException intEx) {
      throw new SystemException("Error waiting for child process to terminate", intEx);
    }
  }

  /** Get the error string from the child process. */
  public String getErrorString() {
    return new BufferedReader(new InputStreamReader(process.getErrorStream()))
        .lines()
        .collect(Collectors.joining(""));
  }
}
