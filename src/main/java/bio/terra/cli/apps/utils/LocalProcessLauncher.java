package bio.terra.cli.apps.utils;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.utils.Printer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** This class provides utility methods for launching local child processes. */
public class LocalProcessLauncher {
  private Process process;
  private Thread stdOutThread;
  private Thread stdErrThread;

  public LocalProcessLauncher() {}

  /**
   * Executes a command in a separate process from the current working directory (i.e. the same
   * place as this Java process is running).
   *
   * @param command the command and arguments to execute
   * @param envVars the environment variables to set or overwrite if already defined
   * @return the process object
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
   * @return the process object
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
      for (Map.Entry<String, String> envVar : envVars.entrySet()) {
        procEnvVars.put(envVar.getKey(), envVar.getValue());
      }
    }
    procBuilder.inheritIO();

    try {
      process = procBuilder.start();
    } catch (IOException ioEx) {
      throw new SystemException("Error launching local process", ioEx);
    }
  }

  /** Stream standard out/err from the child process to the CLI console. */
  public void streamOutputForProcess() {
    Runnable streamStdOut = () -> streamOutput(process.getInputStream(), Printer.getOut());
    stdOutThread = new Thread(streamStdOut);
    stdOutThread.start();

    Runnable streamStdErr = () -> streamOutput(process.getErrorStream(), Printer.getErr());
    stdErrThread = new Thread(streamStdErr);
    stdErrThread.start();
  }

  /**
   * Helper method to stream the child process' output to the CLI console.
   *
   * @param fromStream stream reading from the child process output
   * @param toStream stream writing to the CLI console
   */
  private static void streamOutput(InputStream fromStream, PrintStream toStream) {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(fromStream, Charset.forName("UTF-8")))) {

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        toStream.print(line);
        toStream.flush();
      }
    } catch (IOException ioEx) {
      throw new SystemException("Error streaming output of child process", ioEx);
    }
  }

  /** Block until the child process terminates, then return its exit code. */
  public int waitForTerminate() {
    try {
      return process.waitFor();
    } catch (InterruptedException intEx) {
      throw new SystemException("Error waiting for child process to terminate", intEx);
    }
  }
}
