package bio.terra.cli;

import bio.terra.cli.apps.CommandRunner;
import bio.terra.cli.apps.DockerCommandRunner;
import bio.terra.cli.apps.LocalProcessCommandRunner;
import bio.terra.cli.apps.utils.DockerClientWrapper;
import bio.terra.cli.serialization.disk.DiskConfig;
import bio.terra.cli.utils.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a configuration. An instance of this class is part of the current
 * context or state.
 */
public class Config {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Config.class);

  // launch a browser automatically or not
  private BrowserLaunchOption browserLaunchOption = BrowserLaunchOption.AUTO;

  // how to launch tools: docker image id or tag
  private CommandRunnerOption commandRunnerOption = CommandRunnerOption.DOCKER_CONTAINER;
  private String dockerImageId;

  // maximum number of resources to cache on disk for a single workspace before throwing an error
  // (corresponds to ~1MB cache size on disk)
  private int resourcesCacheSize = DEFAULT_RESOURCES_CACHE_SIZE;

  // log levels for file and stdout
  private Logger.LogLevel consoleLoggingLevel = Logger.LogLevel.OFF;
  private Logger.LogLevel fileLoggingLevel = Logger.LogLevel.INFO;

  public static final int DEFAULT_RESOURCES_CACHE_SIZE = 1000;

  /** Build an instance of this class from the serialized format on disk. */
  public Config(DiskConfig configFromDisk) {
    this.browserLaunchOption = configFromDisk.browserLaunchOption;
    this.commandRunnerOption = configFromDisk.commandRunnerOption;
    this.dockerImageId = configFromDisk.dockerImageId;
    this.resourcesCacheSize = configFromDisk.resourcesCacheSize;
    this.fileLoggingLevel = configFromDisk.fileLoggingLevel;
    this.consoleLoggingLevel = configFromDisk.consoleLoggingLevel;
  }

  /** Build an instance of this class with default values. */
  public Config() {
    this.dockerImageId = getDefaultImageId();
  }

  /** Returns the default image id. */
  public static String getDefaultImageId() {
    // read from the JAR Manifest file
    String fromJarManifest = Config.class.getPackage().getImplementationVersion();
    if (fromJarManifest != null) {
      return fromJarManifest;
    } else {
      // during testing, the JAR may not be built or called, so we use a system property to pass the
      // implementation version instead. this is only expected for testing, not during normal
      // operation.
      logger.warn(
          "Implementation version not defined in the JAR manifest. This is expected when testing, not during normal operation.");
      return System.getProperty("TERRA_JAR_IMPLEMENTATION_VERSION");
    }
  }

  /** Options for handling the browser during the OAuth process. */
  public enum BrowserLaunchOption {
    MANUAL,
    AUTO;
  }

  /** Different ways of running tool/app commands. */
  public enum CommandRunnerOption {
    DOCKER_CONTAINER(new DockerCommandRunner()),
    LOCAL_PROCESS(new LocalProcessCommandRunner());

    private CommandRunner commandRunner;

    CommandRunnerOption(CommandRunner commandRunner) {
      this.commandRunner = commandRunner;
    }

    /** Helper method to get the {@link CommandRunner} sub-class that maps to each enum value. */
    public CommandRunner getRunner() {
      return commandRunner;
    }
  }

  // ====================================================
  // Property get/setters.
  public BrowserLaunchOption getBrowserLaunchOption() {
    return browserLaunchOption;
  }

  public void setBrowserLaunchOption(BrowserLaunchOption browserLaunchOption) {
    this.browserLaunchOption = browserLaunchOption;
    Context.synchronizeToDisk();
  }

  public CommandRunnerOption getCommandRunnerOption() {
    return commandRunnerOption;
  }

  public void setCommandRunnerOption(CommandRunnerOption commandRunnerOption) {
    this.commandRunnerOption = commandRunnerOption;
    Context.synchronizeToDisk();
  }

  public String getDockerImageId() {
    return dockerImageId;
  }

  public void setDockerImageId(String dockerImageId) {
    this.dockerImageId = dockerImageId;
    if (!new DockerClientWrapper().checkImageExists(dockerImageId)) {
      logger.warn("image not found: {}", dockerImageId);
    }
    Context.synchronizeToDisk();
  }

  public int getResourcesCacheSize() {
    return resourcesCacheSize;
  }

  public void setResourcesCacheSize(int resourcesCacheSize) {
    this.resourcesCacheSize = resourcesCacheSize;
    Context.synchronizeToDisk();
  }

  public Logger.LogLevel getConsoleLoggingLevel() {
    return consoleLoggingLevel;
  }

  public void setConsoleLoggingLevel(Logger.LogLevel consoleLoggingLevel) {
    this.consoleLoggingLevel = consoleLoggingLevel;
    Context.synchronizeToDisk();
  }

  public Logger.LogLevel getFileLoggingLevel() {
    return fileLoggingLevel;
  }

  public void setFileLoggingLevel(Logger.LogLevel fileLoggingLevel) {
    this.fileLoggingLevel = fileLoggingLevel;
    Context.synchronizeToDisk();
  }
}
