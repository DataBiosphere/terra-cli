package bio.terra.cli.command.shared.options;

import bio.terra.cli.exception.UserActionableException;

/**
 * Command helper class that defines the --quiet flag for suppressing interactive user input, and
 * prompting the user for a `Y/N`=proceed/abort confirmation if the flag is not specified.
 *
 * <p>Commands that use this option should call {@link #confirmOrThrow()} before any business logic.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class DeletePrompt extends ConfirmationPrompt {
  /**
   * Helper method to generate an interactive confirmation prompt if the `--quiet` option is not
   * specified. Throws a {@link UserActionableException} if the prompt response is negative.
   */
  public void confirmOrThrow() {
    super.confirmOrThrow("Are you sure you want to delete (y/N)?", "Delete aborted.");
  }
}
