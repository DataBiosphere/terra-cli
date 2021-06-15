package harness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.command.Main;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    System.out.println("COMMAND: " + String.join(" ", args));
    int exitCode = Main.runCommand(args);

    // log the stdout and stderr to the console
    String stdOutStr = stdOut.toString(StandardCharsets.UTF_8);
    String stdErrStr = stdErr.toString(StandardCharsets.UTF_8);
    System.out.println(stdOutStr);
    if (!stdErrStr.isEmpty()) {
      System.out.println("STDERR --------------");
      System.out.println(stdErrStr);
    }

    // return a result object with all the command outputs
    return new Result(exitCode, stdOutStr, stdErrStr);
  }

  /**
   * Helper method to run a command and check its exit code matches that specified. Returns the
   * standard error string for validating an error message.
   */
  public static String runCommandExpectExitCode(int exitCode, String... args) {
    Result cmd = runCommand(args);
    assertEquals(exitCode, cmd.exitCode, "exit code = " + exitCode);
    return cmd.stdErr;
  }

  /** Helper method to run a command and check its exit code is 0=success. */
  public static void runCommandExpectSuccess(String... args) {
    Result cmd = runCommand(args);
    assertEquals(0, cmd.exitCode, "exit code = success");
  }

  /**
   * Helper method to run a command, check its exit code is 0=success, and read what's written to
   * standard out into a Java object. Adds `--format=json` to the argument list.
   */
  public static <T> T runAndParseCommandExpectSuccess(Class<T> objectType, String... args)
      throws JsonProcessingException {
    Result cmd = runCommand(addFormatJsonArg(args));
    assertEquals(0, cmd.exitCode, "exit code = success");
    return cmd.readObjectFromStdOut(objectType);
  }

  /**
   * Helper method to run a command, check its exit code is 0=success, and read what's written to
   * standard out into a Java object. Adds `--format=json` to the argument list.
   */
  public static <T> T runAndParseCommandExpectSuccess(TypeReference<T> objectType, String... args)
      throws JsonProcessingException {
    Result cmd = runCommand(addFormatJsonArg(args));
    assertEquals(0, cmd.exitCode, "exit code = success");
    return cmd.readObjectFromStdOut(objectType);
  }

  /** Add the `--format=json` argument to the end of the arguments list. */
  private static String[] addFormatJsonArg(String... args) {
    List<String> argsWithFormatJson = new ArrayList<>(Arrays.asList(args));
    argsWithFormatJson.add("--format=json");
    return argsWithFormatJson.toArray(new String[0]);
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

    /** Convert what's written to standard out into a Java object. */
    private <T> T readObjectFromStdOut(Class<T> objectType) throws JsonProcessingException {
      return objectMapper.readValue(stdOut, objectType);
    }

    /** Convert what's written to standard out into a Java object. */
    private <T> T readObjectFromStdOut(TypeReference<T> objectType) throws JsonProcessingException {
      return objectMapper.readValue(stdOut, objectType);
    }
  }

  /**
   * Helper method to get the absolute path to a file in the test/resources/testinputs directory.
   */
  public static Path getPathFromFileName(String name) {
    Path filePath =
        Path.of(TestCommand.class.getClassLoader().getResource("testinputs/" + name).getPath())
            .toAbsolutePath();
    return filePath;
  }
}
