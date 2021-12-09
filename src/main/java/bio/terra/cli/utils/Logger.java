package bio.terra.cli.utils;

import static ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME;

import bio.terra.cli.businessobject.Context;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import org.slf4j.LoggerFactory;

/**
 * Utility class to initialize and configure a Logback context and loggers for the main CLI app.
 * Requires an already-loaded GlobalContext for defining logging output files and loading config
 * properties.
 */
public class Logger {
  private static final String LOG_FORMAT =
      "%d{yyyy-MM-dd HH:mm:ss.SSS zz} [%thread] %-5level %logger{50} - %msg%n";

  private static final long MAX_FILE_SIZE = 50 * FileSize.MB_COEFFICIENT; // 5 MB
  private static final int MAX_NUM_FILES = 5;

  /**
   * Setup a file and console appender for the root logger. Each may use a different logging level,
   * as specified in the global context.
   */
  @SuppressFBWarnings(
      value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
      justification =
          "An NPE would only happen here if there was an error getting the global context directory,"
              + " and an exception would have been thrown earlier when we first read in the global "
              + "context file anyway.")
  public static void setupLogging(LogLevel consoleLoggingLevel, LogLevel fileLoggingLevel) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.reset();

    // turn on logging for the logger for errors only
    StatusPrinter.printIfErrorsOccured(loggerContext);

    // build the rolling file appender
    RollingFileAppender rollingFileAppender = new RollingFileAppender();
    rollingFileAppender.setName("RollingFileAppender");
    rollingFileAppender.setContext(loggerContext);
    rollingFileAppender.setFile(Context.getLogFile().toString());

    TimeBasedRollingPolicy timeBasedRollingPolicy = new TimeBasedRollingPolicy();
    timeBasedRollingPolicy.setContext(loggerContext);
    timeBasedRollingPolicy.setMaxHistory(MAX_NUM_FILES);
    timeBasedRollingPolicy.setTotalSizeCap(new FileSize(MAX_FILE_SIZE));
    timeBasedRollingPolicy.setFileNamePattern("%d-terra.log");
    // Context.getLogFile().getParent().resolve("%d{yyyy-MM-dd}-terra.log").toString());
    rollingFileAppender.setRollingPolicy(timeBasedRollingPolicy);
    rollingFileAppender.setTriggeringPolicy(timeBasedRollingPolicy);

    // make sure to start the policies after the cross-references between the policy and appender
    // have been set
    timeBasedRollingPolicy.start();

    setupEncoderAndFilter(rollingFileAppender, loggerContext, fileLoggingLevel.getLogLevelImpl());
    rollingFileAppender.start();

    // build the console appender
    ConsoleAppender consoleAppender = new ConsoleAppender();
    consoleAppender.setName("ConsoleAppender");
    consoleAppender.setContext(loggerContext);
    setupEncoderAndFilter(consoleAppender, loggerContext, consoleLoggingLevel.getLogLevelImpl());
    consoleAppender.start();

    // on the root logger, clear any existing appenders and attach the two created above
    // also set the root log level to ALL, so that each appender can set its level independently.
    // if we don't sent the root log level to ALL, then it acts as a second filter for any messages
    // below its default level DEBUG.
    ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.ALL);
    rootLogger.detachAndStopAllAppenders();
    rootLogger.addAppender(rollingFileAppender);
    rootLogger.addAppender(consoleAppender);

    // if a process is too short-lived, then the policy may not check if it should rollover.
    // each CLI command invocation is a new process (i.e. new JVM) and may be very short-lived.
    // so, include an additional manual check here on startup, to see if we should roll over the
    // file based on its size.
    File activeFile = new File(timeBasedRollingPolicy.getActiveFileName());
    if (activeFile.length() > MAX_FILE_SIZE) {
      rollingFileAppender.rollover();
    }

    // StatusPrinter.print(loggerContext); // helpful for debugging
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
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern(LOG_FORMAT);
    encoder.start();
    appender.setEncoder(encoder);

    // filter out any logs that are below the logging level specified in the global context
    appender.clearAllFilters();
    ThresholdFilter thresholdFilter = new ThresholdFilter();
    thresholdFilter.setLevel(loggingLevel.levelStr);
    thresholdFilter.start();
    appender.addFilter(thresholdFilter);
  }

  /**
   * Wrapper class around the ch.qos.logback.classic.Level class.
   *
   * <p>The purpose of this wrapper is to not leak logger implementation details outside of this
   * class. The rest of the codebase should use the SLF4J facade only, and not need to know about
   * the internals of the implementation. Importantly, the SLF4J facade does not include the OFF
   * level, but the implementation we use does.
   *
   * <p>Without this wrapper class, we'd leak logger implementation into:
   *
   * <p>- GlobalContext, including the serialized version persisted in a file.
   * ch.qos.logback.classic.Level is an object, not a string, and we don't want this file to include
   * logger implementation-specific object structure. That would make it harder to change later (i.e
   * because if we replace the cho.qos library with another one, then we won't have the appropriate
   * Level class any more to do the deserialization)
   *
   * <p>- Config command to set the log level(s), including displaying the possible values.
   */
  public enum LogLevel {
    OFF,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
    ALL;

    private Level logLevelImpl;

    LogLevel() {
      this.logLevelImpl = Level.valueOf(this.toString());
    }

    public Level getLogLevelImpl() {
      return logLevelImpl;
    }
  }
}
