package bio.terra.cli.command.helperclasses.options;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.utils.Printer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.function.Consumer;
import picocli.CommandLine;

/**
 * Command helper class that defines the --format flag and provides utility methods to printing a
 * return value out in different formats.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class Format {

  @CommandLine.Option(
      names = "--format",
      defaultValue = "text",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
      description = "Set the format for printing command output: ${COMPLETION-CANDIDATES}")
  private FormatOptions format;

  /** This enum specifies the format options for printing the command output. */
  private enum FormatOptions {
    json,
    text;
  }

  /**
   * This method calls the {@link #printJson} method if the --format flag is set to JSON. Otherwise,
   * it calls the {@link #printText} method, passing the return value object as an argument.
   *
   * @param returnValue command return value
   */
  public <T> void printReturnValue(T returnValue) {
    printReturnValue(returnValue, Format::printText, Format::printJson);
  }

  /**
   * This method calls the {@link #printJson} method if the --format flag is set to JSON. Otherwise,
   * it calls the given printTextFunction.
   *
   * @param returnValue command return value
   * @param printTextFunction reference to function that accepts the command return value and prints
   *     it out in text format
   */
  public <T> void printReturnValue(T returnValue, Consumer<T> printTextFunction) {
    printReturnValue(returnValue, printTextFunction, Format::printJson);
  }

  /**
   * This method calls the given printJsonFunction if the --format flag is set to JSON. Otherwise,
   * it calls the given printTextFunction.
   *
   * @param returnValue command return value
   * @param printTextFunction reference to function that accepts the command return value and prints
   *     it out in text format
   * @param printJsonFunction reference to function that accepts the command return value and prints
   *     it out in JSON format
   */
  public <T> void printReturnValue(
      T returnValue, Consumer<T> printTextFunction, Consumer<T> printJsonFunction) {
    if (format == FormatOptions.json) {
      printJsonFunction.accept(returnValue);
    } else {
      printTextFunction.accept(returnValue);
    }
  }

  /**
   * Default implementation of printing this command's return value in JSON format. This method uses
   * Jackson for serialization.
   *
   * @param returnValue command return value
   */
  public static <T> void printJson(T returnValue) {
    // use Jackson to map the object to a JSON-formatted text block
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    try {
      Printer.getOut().println(objectWriter.writeValueAsString(returnValue));
    } catch (JsonProcessingException jsonEx) {
      throw new SystemException("Error JSON-formatting the command return value.", jsonEx);
    }
  }

  /**
   * Default implementation of printing the return value. This method uses the {@link
   * Object#toString} method of the return value object, and prints nothing if this object is null.
   *
   * @param returnValue command return value
   */
  public static <T> void printText(T returnValue) {
    if (returnValue != null) {
      Printer.getOut().println(returnValue.toString());
    }
  }
}
