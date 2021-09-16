package bio.terra.cli.command.shared.options;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.UserIO;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import picocli.CommandLine;

/**
 * Command helper class that defines the --quiet flag for suppressing interactive user input, and
 * prompting the user for a `Y/N`=proceed/abort confirmation if the flag is not specified.
 *
 * <p>Commands that use this option should call {@link #confirmOrThrow()} before any business logic.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class DeletePrompt {

  @CommandLine.Option(names = "--quiet", description = "Suppress interactive prompt and delete.")
  private boolean quiet;

  public void confirmOrThrow() {
    confirmOrThrow("");
  }
  /**
   * Helper method to generate an interactive confirmation prompt if the `--quiet` option is not
   * specified. Throws a {@link UserActionableException} if the prompt response is negative.
   *
   * @param description - A short piece of text describing the object to be deleted and any relevant
   *     children, internal state, etc.
   */
  public void confirmOrThrow(String description) {
    boolean promptReturnedYes = true; // default to Y=yes if user input is suppressed
    PrintStream OUT = UserIO.getOut();
    if (!quiet) {
      OUT.print(description + "\nAre you sure you want to delete (y/N)? ");
      Scanner reader = new Scanner(UserIO.getIn(), StandardCharsets.UTF_8);
      String deletePrompt = reader.nextLine();
      promptReturnedYes =
          deletePrompt.equalsIgnoreCase("y") || deletePrompt.equalsIgnoreCase("yes");
    }

    if (!promptReturnedYes) {
      throw new UserActionableException("Delete aborted.");
    }
  }
}
