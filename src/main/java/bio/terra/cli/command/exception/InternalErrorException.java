package bio.terra.cli.command.exception;

/**
 * Custom exception class for internal exceptions. These represent errors that the user cannot fix.
 * (e.g. "Error refreshing access token" when logging in).
 *
 * <p>In contrast to UserFacingExceptions, throwing an InternalErrorException will print information
 * about the cause and will point users to the log file for more information.
 */
public class InternalErrorException extends RuntimeException {
  /**
   * Constructs an exception with the given message. The cause is set to null.
   *
   * @param message description of error that may help with debugging
   */
  public InternalErrorException(String message) {
    super(message);
  }

  /**
   * Constructs an exception with the given message and cause.
   *
   * @param message description of error that may help with debugging
   * @param cause underlying exception that can be logged for debugging purposes
   */
  public InternalErrorException(String message, Throwable cause) {
    super(message, cause);
  }
}
