package harness;

import bio.terra.cli.command.Main;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for executing commands and reading their outputs during testing. This class is
 * intended for unit tests because it calls the commands directly in Java.
 */
public class TestCommand {

  private static ObjectMapper objectMapper = new ObjectMapper();

  private TestCommand() {}

  /**
   * Call the top-level Main class to execute a command. Return the exit code, standard out and
   * standard error.
   *
   * @param args sub-commands and arguments (e.g. "auth", "status" for `terra auth status`).
   */
  public static Result runCommand(String... args) {
    // initialize the singleton Printer object with byte streams, to redirect the command stdout and
    // stderr from the console
    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    Printer.initialize(
        new PrintStream(stdOut, true, StandardCharsets.UTF_8),
        new PrintStream(stdErr, true, StandardCharsets.UTF_8));

    // execute the command from the top-level Main class
    int exitCode = Main.runCommand(args);

    // return a result object with all the command outputs
    return new Result(
        exitCode, stdOut.toString(StandardCharsets.UTF_8), stdErr.toString(StandardCharsets.UTF_8));
  }

  /** Helper method to convert what's written to standard out into a Java object. */
  public static <T> T readObjectFromStdOut(Result cmd, Class<T> objectType)
      throws JsonProcessingException {
    return objectMapper.readValue(cmd.stdOut, objectType);
  }

  /** Helper class to return all outputs of a command: exit code, standard out, standard error. */
  public static class Result {
    public final int exitCode;
    public final String stdOut;
    public final String stdErr;

    public Result(int exitCode, String stdOut, String stdErr) {
      this.exitCode = exitCode;
      this.stdOut = stdOut;
      this.stdErr = stdErr;
    }
  }
}
