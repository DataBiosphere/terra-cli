package bio.terra.cli.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProcessUtils {

  private static Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);

  private ProcessUtils() {}

  /**
   * Executes a command in a separate process.
   *
   * @param cmdArgs a list of the command line arguments=
   * @return a List of the lines written to stdout
   * @throws IOException
   */
  public static List<String> executeCommand(String cmd, List<String> cmdArgs) throws IOException {
    // build and run process
    cmdArgs.add(0, cmd);
    ProcessBuilder procBuilder = new ProcessBuilder(cmdArgs);
    Process proc = procBuilder.start();
    LOG.debug("started process: " + String.join(" ", cmdArgs));

    // read in all lines written to stdout
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(proc.getInputStream(), Charset.defaultCharset()));
    String outputLine;
    List<String> outputLines = new ArrayList<>();
    while ((outputLine = bufferedReader.readLine()) != null) {
      LOG.trace(outputLine);
      outputLines.add(outputLine);
    }
    bufferedReader.close();

    return outputLines;
  }
}
