package bio.terra.cli.exception;

/**
 * Custom exception class for user actionable exceptions. These represent errors that the user can
 * fix. (e.g. "Invalid bucket path" when creating a data reference).
 *
 * <p>In contrast to SystemExceptions, throwing a UserActionableException will
 *
 * <p>-NOT print information about the cause or point users to the log file for more information
 *
 * <p>-use a less scary font when printing to the terminal (e.g. regular blue text).
 */
public class UserActionableException extends RuntimeException {
  /**
   * Constructs an exception with the given message. The cause is set to null.
   *
   * @param message string to display to the user
   */
  public UserActionableException(String message) {
    super(message);
  }

  /**
   * Constructs an exception with the given message and cause.
   *
   * @param message string to display to the user
   * @param cause underlying exception that can be logged for debugging purposes
   */
  public UserActionableException(String message, Throwable cause) {
    super(message, cause);
  }
}
