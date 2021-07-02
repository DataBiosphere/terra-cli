package bio.terra.cli.command.shared.options;

import bio.terra.cli.utils.Printer;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import picocli.CommandLine;

/**
 * Command helper class that defines the --quiet flag for suppressing interactive user input, and
 * prompting the user for a `Y/N`=proceed/abort confirmation if the flag is not specified.
 *
 * <p>Commands that use this option should call {@link #promptReturnedYes()} before any business
 * logic. e.g. if (!deletePromptOption.promptReturnedYes()) { return; }
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class DeletePrompt {

  @CommandLine.Option(names = "--quiet", description = "Suppress interactive prompt and delete.")
  private boolean quiet;

  /**
   * Helper method to generate an interactive confirmation prompt if the `--quiet` option is not
   * specified.
   */
  public boolean promptReturnedYes() {
    boolean promptReturnedYes = true; // default to Y=yes if user input is suppressed
    PrintStream OUT = Printer.getOut();
    if (!quiet) {
      OUT.print("Are you sure you want to delete (y/N)? ");
      Scanner reader = new Scanner(Printer.getIn(), StandardCharsets.UTF_8);
      String deletePrompt = reader.nextLine();
      promptReturnedYes =
          deletePrompt.equalsIgnoreCase("y") || deletePrompt.equalsIgnoreCase("yes");
    }

    if (!promptReturnedYes) {
      OUT.println("Aborting delete.");
    }
    return promptReturnedYes;
  }
}
