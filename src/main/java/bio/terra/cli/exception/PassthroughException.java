package bio.terra.cli.exception;

/**
 * Custom exception class for exceptions thrown by apps.
 *
 * <p>Throwing a PassthroughException will
 *
 * <p>-print information about the cause and point users to the log file for more information
 *
 * <p>-use a normal font when printing to the terminal (e.g. same as other app output).
 */
public class PassthroughException extends RuntimeException {
  private int exitCode;

  /**
   * Constructs an exception with the given exit code. The cause is set to null.
   *
   * @param exitCode exit code returned by the app, to be passed through as the CLI exit code
   */
  public PassthroughException(int exitCode) {
    super();
    this.exitCode = exitCode;
  }

  /**
   * Constructs an exception with the given exit code and message. The cause is set to null.
   *
   * @param exitCode exit code returned by the app, to be passed through as the CLI exit code
   * @param message description of error that may help with debugging
   */
  public PassthroughException(int exitCode, String message) {
    super(message);
    this.exitCode = exitCode;
  }

  /** Get the exit code returned by the app, to be passed through as the CLI exit code. */
  public int getExitCode() {
    return exitCode;
  }
}
