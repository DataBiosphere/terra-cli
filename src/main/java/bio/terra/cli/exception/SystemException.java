package bio.terra.cli.exception;

/**
 * Custom exception class for system or internal exceptions. These represent errors that the user
 * cannot fix. (e.g. "Error refreshing access token" when logging in).
 *
 * <p>In contrast to UserActionableExceptions, throwing a SystemException will
 *
 * <p>-print information about the cause and point users to the log file for more information
 *
 * <p>-use a bolder font when printing to the terminal (e.g. ERROR in bold red text).
 */
public class SystemException extends RuntimeException {
  /**
   * Constructs an exception with the given message. The cause is set to null.
   *
   * @param message description of error that may help with debugging
   */
  public SystemException(String message) {
    super(message);
  }

  /**
   * Constructs an exception with the given message and cause.
   *
   * @param message description of error that may help with debugging
   * @param cause underlying exception that can be logged for debugging purposes
   */
  public SystemException(String message, Throwable cause) {
    super(message, cause);
  }
}
