package bio.terra.cli.command.exception;

/**
 * Custom exception class for user-facing exceptions. These represent errors that the user can fix.
 * (e.g. "Invalid bucket path" when creating a data reference).
 *
 * <p>In contrast to InternalErrorExceptions, throwing a UserFacingException will not print any
 * information about the cause and will not point users to the log file for more information.
 */
public class UserFacingException extends RuntimeException {
  /**
   * Constructs an exception with the given message. The cause is set to null.
   *
   * @param message string to display to the user
   */
  public UserFacingException(String message) {
    super(message);
  }

  /**
   * Constructs an exception with the given message and cause.
   *
   * @param message string to display to the user
   * @param cause underlying exception that can be logged for debugging purposes
   */
  public UserFacingException(String message, Throwable cause) {
    super(message, cause);
  }
}
