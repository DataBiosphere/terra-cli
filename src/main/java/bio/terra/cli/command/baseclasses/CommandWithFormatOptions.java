package bio.terra.cli.command.baseclasses;

import bio.terra.cli.command.exception.SystemException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import picocli.CommandLine;

/**
 * Base class for commands that support multiple output formats. This class handles:
 *
 * <p>- defining the --format flag and its possible values
 *
 * <p>- calling the corresponding print__ method depending on the value of the flag
 *
 * <p>Sub-classes can override any of the print__ methods with a custom implementation.
 *
 * @param <T> class type of the command's return value, should be a POJO class that can be
 *     serialized to JSON
 */
public abstract class CommandWithFormatOptions<T> extends BaseCommand<T> {
  @CommandLine.Option(
      names = "--format",
      hidden = true,
      description = "Set the format for printing command output: ${COMPLETION-CANDIDATES}")
  private FormatOptions format;

  /** This enum specifies the format options for printing the command output. */
  private enum FormatOptions {
    json,
    text;
  }

  /**
   * This method calls the {@link #printJson} method if the --format flag is set to JSON. Otherwise,
   * it calls the {@link #printText} method of the return value object.
   *
   * @param returnValue command return value
   */
  @Override
  protected void printReturnValue(T returnValue) {
    if (format == FormatOptions.json) {
      printJson(returnValue);
    } else {
      printText(returnValue);
    }
  }

  /**
   * Default implementation of printing this command's return value in JSON format. This method uses
   * Jackson for serialization.
   *
   * @param returnValue command return value
   */
  protected void printJson(T returnValue) {
    // use Jackson to map the object to a JSON-formatted text block
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    try {
      out.println(objectWriter.writeValueAsString(returnValue));
    } catch (JsonProcessingException jsonEx) {
      throw new SystemException("Error JSON-formatting the command return value.", jsonEx);
    }
  }

  /**
   * Default implementation of printing this command's return value in text format. This method
   * calls the {@link Object#toString} method of the return value object.
   *
   * @param returnValue command return value
   */
  protected void printText(T returnValue) {
    super.printReturnValue(returnValue);
  }
}
