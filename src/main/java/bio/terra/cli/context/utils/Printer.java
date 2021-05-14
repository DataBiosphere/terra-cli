package bio.terra.cli.context.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Singleton class for holding a reference to output streams (e.g. stdout, stderr). The purpose of
 * holding these references in a single place is so that we can write output throughout the codebase
 * without passing around the output streams from the top-level command classes.
 */
public class Printer {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Printer.class);

  public static final PrintStream DEFAULT_OUT_STREAM = System.out;
  public static final PrintStream DEFAULT_ERR_STREAM = System.err;

  private final PrintStream out;
  private final PrintStream err;

  private static Printer printer;

  /** Constructor that initializes the STREAM with default values. */
  private Printer() {
    this(DEFAULT_OUT_STREAM, DEFAULT_ERR_STREAM);
  }

  /** Constructor that initializes the printer with the specified output streams. */
  private Printer(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
  }

  /**
   * This method should be called exactly once to setup printing in the top-level command (i.e.
   * Main). It sets up the following:
   *
   * <p>- Sets the output stream pointers on the top-level command. This will recursively set the
   * pointers on all sub-commands also.
   *
   * <p>- Initializes the singleton Printer object that holds pointers to the output streams for use
   * elsewhere in the codebase (i.e. outside of command classes). This method uses default values
   * for these output streams.
   *
   * @param cmd picocli top-level command line object that holds pointers to the output streams
   */
  public static void setupPrinting(CommandLine cmd) {
    if (printer == null) {
      printer = new Printer();
    } else {
      logger.warn("Printing setup called multiple times.");
    }
    cmd.setOut(getPrintWriter(printer.out));
    cmd.setErr(getPrintWriter(printer.err));
  }

  /**
   * Utility method to get the output stream from the singleton.
   *
   * @return stream to write output (e.g. stdout)
   */
  public static PrintStream getOut() {
    if (printer == null) {
      logger.warn("Attempt to access printer output stream before setup.");
      return DEFAULT_OUT_STREAM;
    }
    return printer.out;
  }

  /**
   * Utility method to get the error stream from the singleton.
   *
   * @return stream to write errors and running status (e.g. stderr)
   */
  public static PrintStream getErr() {
    if (printer == null) {
      logger.warn("Attempt to access printer error stream before setup.");
      return DEFAULT_ERR_STREAM;
    }
    return printer.err;
  }

  /** Utility method to get a UTF-8 encoded character output stream from a raw byte stream. */
  private static PrintWriter getPrintWriter(PrintStream printStream) {
    return new PrintWriter(printStream, true, Charset.forName("UTF-8"));
  }
}
