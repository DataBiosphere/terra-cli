package bio.terra.cli.command;

import bio.terra.cli.context.GlobalContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Base class for all commands that includes any global options. This class handles reading in the
 * current context, executing the command, and printing the return value. Sub-classes define the
 * implementation of the command execution, the structure of the {@link BaseReturnValue} POJO class,
 * and the implementation of printing the return value out in text format.
 *
 * @param <T> the {@link BaseReturnValue} sub-class that defines the command's return value
 */
public abstract class BaseCommand<T extends BaseCommand.BaseReturnValue>
    implements Callable<Integer> {

  protected GlobalContext globalContext;

  @CommandLine.Option(
      names = "--format",
      hidden = true,
      description = "Set the format for printing command output: ${COMPLETION-CANDIDATES}")
  private FormatOptions format;

  @Override
  public Integer call() {
    // get the return value object
    T returnValue = getReturnValue();

    // print the return value
    if (format == FormatOptions.json) {
      returnValue.printJSON();
    } else {
      returnValue.printText();
    }

    // set the command exit code
    return 0;
  }

  /**
   * Convenience method for getting the {@link BaseReturnValue} object from a command. This is
   * useful for commands that call other commands (e.g. config list calls the config get-value
   * commands).
   *
   * @return return value object
   */
  public T getReturnValue() {
    // read in the current context
    globalContext = GlobalContext.readFromFile();

    // execute the command
    return execute();
  }

  /** This enum specifies the format options for printing the command output. */
  private enum FormatOptions {
    json,
    text;
  }

  /**
   * Required override for executing this command and returning an instance of the {@link
   * BaseReturnValue} object.
   *
   * @return {@link BaseReturnValue} object
   */
  public abstract T execute();

  // TODO: is there a way to pull this from Main.class (e.g. pass cmd.getOut)?
  protected static final PrintStream output = System.out;

  /**
   * This abstract base class represents a command's return value. Sub-classes of {@link
   * BaseCommand} must also implement a sub-class of {@link BaseReturnValue}. Typically, this will
   * be a simple POJO class, with a custom implementation of printText.
   */
  public abstract static class BaseReturnValue {
    /**
     * Default implementation of printing this {@link BaseReturnValue} in JSON format. This method
     * uses Jackson for serialization.
     */
    public void printJSON() {
      // use Jackson to map the object to a JSON-formatted text block
      ObjectMapper objectMapper = new ObjectMapper();
      ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
      try {
        output.println(objectWriter.writeValueAsString(this));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }

    /** Required override for printing this {@link BaseReturnValue} in text format. */
    public abstract void printText();
  }
}
