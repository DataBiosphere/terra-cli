package bio.terra.cli.context.utils;

import bio.terra.cli.context.GlobalContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

/** Utility methods for logging. */
public class Logger {
  private static String LOG_FORMAT = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n";

  private final GlobalContext globalContext;

  /**
   * Constructor for class that sets up logging.
   *
   * @param globalContext the global context object
   */
  public Logger(GlobalContext globalContext) {
    this.globalContext = globalContext;
  }

  /**
   * Setup a file and console appender for the root logger. Each may use a different logging level,
   * as specified in the global context.
   */
  public void setupLogging() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    // turn on logging for the logger for errors only
    StatusPrinter.printIfErrorsOccured(loggerContext);

    // build the file appender
    FileAppender fileAppender = new FileAppender();
    fileAppender.setName("FileAppender");
    fileAppender.setFile(GlobalContext.getLogFile().toString());
    setupEncoderAndFilter(fileAppender, loggerContext, globalContext.fileLoggingLevel);

    // build the console appender
    ConsoleAppender consoleAppender = new ConsoleAppender();
    consoleAppender.setName("ConsoleAppender");
    setupEncoderAndFilter(consoleAppender, loggerContext, globalContext.consoleLoggingLevel);

    // on the root logger, clear any existing appenders and attach the two created above
    ch.qos.logback.classic.Logger rootLogger =
        loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAndStopAllAppenders();
    rootLogger.addAppender(fileAppender);
    rootLogger.addAppender(consoleAppender);
  }

  /**
   * Helper method to setup the encoder and filter for the given appender. This setup is common to
   * all appenders.
   *
   * @param appender appender object to setup
   * @param loggerContext global logger context object
   * @param loggingLevel logging level particular to this appender
   */
  private static void setupEncoderAndFilter(
      OutputStreamAppender appender, LoggerContext loggerContext, Level loggingLevel) {
    appender.setContext(loggerContext);

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern(LOG_FORMAT);
    encoder.start();
    appender.setEncoder(encoder);

    // filter out any logs that are below the fileLoggingLevel specified in the global context
    appender.clearAllFilters();
    appender.addFilter(
        new Filter<ILoggingEvent>() {
          @Override
          public FilterReply decide(final ILoggingEvent event) {
            return event.getLevel().isGreaterOrEqual(loggingLevel)
                ? FilterReply.ACCEPT
                : FilterReply.DENY;
          }
        });

    appender.start();
  }
}
