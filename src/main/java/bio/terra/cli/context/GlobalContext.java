package bio.terra.cli.context;

import static bio.terra.cli.context.GlobalContext.CommandRunners.DOCKER_CONTAINER;
import static bio.terra.cli.context.utils.Logger.LogLevel;

import bio.terra.cli.apps.CommandRunner;
import bio.terra.cli.apps.DockerCommandRunner;
import bio.terra.cli.apps.LocalProcessCommandRunner;
import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.utils.JacksonMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This POJO class represents an instance of the Terra CLI global context. This is intended
 * primarily for authentication and connection-related context values that will span multiple
 * workspaces.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GlobalContext {
  private static final Logger logger = LoggerFactory.getLogger(GlobalContext.class);

  // global auth context = current Terra user
  private TerraUser terraUser;

  // global server context = service uris, environment name
  private Server server;

  // global workspace context = current workspace
  private Workspace workspace;

  // global config context
  //   - flag indicating whether to launch a browser automatically or not
  private BrowserLaunchOption browserLaunchOption = BrowserLaunchOption.AUTO;

  //   - flag for how to launch tools, docker image id or tag
  private CommandRunners commandRunnerOption = DOCKER_CONTAINER;
  private String dockerImageId;

  //   - maximum number of resources to cache on disk for a single workspace before throwing an
  // error
  //     (corresponds to ~1MB cache size on disk)
  private int resourcesCacheSize = DEFAULT_RESOURCES_CACHE_SIZE;
  public static final int DEFAULT_RESOURCES_CACHE_SIZE = 1000;

  //   - log levels for file and stdout
  private LogLevel fileLoggingLevel = LogLevel.INFO;
  private LogLevel consoleLoggingLevel = LogLevel.OFF;

  //   - env var name to optionally override where the context is persisted on disk
  private static final String CONTEXT_DIR_OVERRIDE_NAME = "TERRA_CONTEXT_PARENT_DIR";

  // file paths related to persisting the global context on disk
  private static final String GLOBAL_CONTEXT_DIRNAME = ".terra";
  private static final String GLOBAL_CONTEXT_FILENAME = "global-context.json";
  private static final String PET_KEYS_DIRNAME = "pet-keys";
  private static final String LOGS_DIRNAME = "logs";
  private static final String LOG_FILENAME = "terra.log";

  private static GlobalContext globalContext;

  private GlobalContext() {}

  /**
   * Return the singleton instance of this class. If the instance is not already defined, then read
   * it in from a JSON-formatted file in the global context directory. If there is no existing file,
   * then build an object populated with default values.
   *
   * <p>Note: DO NOT put any logger statements in this function. Because we setup the loggers using
   * the logging levels specified in the global context, the loggers have not been setup when we
   * first call this function.
   *
   * @return an instance of this class
   */
  public static GlobalContext get() {
    if (globalContext != null) {
      return globalContext;
    }

    try {
      // try to read in an instance of the global context file
      globalContext =
          JacksonMapper.readFileIntoJavaObject(
              getGlobalContextFile().toFile(), GlobalContext.class);
    } catch (IOException ioEx) {
      // file not found is a common error here (e.g. first time running the CLI, there will be no
      // pre-existing global context file). we handle this by returning an object populated with
      // default values below. so, no need to log or throw the exception returned here.
      globalContext = new GlobalContext();
      globalContext.server = Server.getDefault();
      globalContext.dockerImageId = DockerCommandRunner.defaultImageId();
    }

    return globalContext;
  }

  // ====================================================
  // Property get/setters.

  /** Getter for the current Terra user. Returns empty if no current user is defined. */
  @JsonIgnore
  public Optional<TerraUser> getCurrentTerraUser() {
    return Optional.ofNullable(terraUser);
  }

  /** Utility method that throws an exception if the current Terra user is not defined. */
  public TerraUser requireCurrentTerraUser() {
    Optional<TerraUser> terraUserOpt = getCurrentTerraUser();
    if (!terraUserOpt.isPresent()) {
      throw new UserActionableException("The current Terra user is not defined. Login required.");
    }
    return terraUserOpt.get();
  }

  /** Set the current terra user. Persists on disk. */
  public void setCurrentTerraUser(TerraUser terraUser) {
    boolean userUnchanged =
        (this.terraUser == null && terraUser == null)
            || (this.terraUser != null
                && terraUser != null
                && this.terraUser.getEmail().equals(terraUser.getEmail()));
    this.terraUser = terraUser;

    // only persist the user if the user changed
    // otherwise, this method just updates the credentials stored on the TerraUser object, which are
    // not persisted to disk
    if (!userUnchanged) {
      writeToFile();
    }
  }

  /** Clear the current terra user. Persists on disk. */
  public void unsetCurrentTerraUser() {
    setCurrentTerraUser(null);
  }

  /** Getter for the current server. */
  public Server getServer() {
    return server;
  }

  /** Setter for the current Terra server. Persists on disk. */
  public void updateServer(Server server) {
    logger.info("Updating server from {} to {}.", this.server.name, server.name);
    this.server = server;

    writeToFile();
  }

  /** Getter for the current workspace. Returns empty if no current workspace is defined. */
  @JsonIgnore
  public Optional<Workspace> getCurrentWorkspace() {
    return Optional.ofNullable(workspace);
  }

  /** Utility method that throws an exception if the current workspace is not defined. */
  public Workspace requireCurrentWorkspace() {
    Optional<Workspace> workspaceOpt = getCurrentWorkspace();
    if (!workspaceOpt.isPresent()) {
      throw new UserActionableException("The current workspace is not defined.");
    }
    return workspaceOpt.get();
  }

  /** Sets the current workspace. Persists on disk */
  public void setCurrentWorkspace(Workspace workspace) {
    logger.info(
        "Updating workspace from {} to {}.",
        this.workspace == null ? "undefined" : this.workspace.id,
        workspace.id);
    this.workspace = workspace;

    writeToFile();
  }

  /** Clear the current workspace. Persists on disk. */
  public void unsetCurrentWorkspace() {
    this.workspace = null;
    writeToFile();
  }

  /**
   * This enum configures the browser for authentication. Currently, there are only two possible
   * values, so this could be a boolean flag instead. However, this may change in the future and
   * it's useful to have the flag values mapped to strings that can be displayed to the user.
   */
  public enum BrowserLaunchOption {
    MANUAL,
    AUTO;
  }

  /** Getter for the browser launch option. */
  public BrowserLaunchOption getBrowserLaunchOption() {
    return this.browserLaunchOption;
  }

  /**
   * Setter for the browser launch option. Persists on disk.
   *
   * @param browserLaunchOption new value for the browser launch option
   */
  public void updateBrowserLaunchOption(BrowserLaunchOption browserLaunchOption) {
    logger.info(
        "Updating browser launch flag from {} to {}.",
        this.browserLaunchOption,
        browserLaunchOption);
    this.browserLaunchOption = browserLaunchOption;

    writeToFile();
  }

  /** This enum defines the different ways of running tool/app commands. */
  public enum CommandRunners {
    DOCKER_CONTAINER(new DockerCommandRunner()),
    LOCAL_PROCESS(new LocalProcessCommandRunner());

    private CommandRunner commandRunner;

    CommandRunners(CommandRunner commandRunner) {
      this.commandRunner = commandRunner;
    }

    /** Helper method to get the {@link CommandRunner} sub-class that maps to each enum value. */
    public CommandRunner getCommandRunner() {
      return commandRunner;
    }
  }

  /** Getter for the command runner option. */
  public CommandRunners getCommandRunnerOption() {
    return commandRunnerOption;
  }

  /**
   * Setter for the command runner option. Persists on disk.
   *
   * @param commandRunnerOption new value for the command runner option
   */
  public void updateCommandRunnerOption(CommandRunners commandRunnerOption) {
    logger.info(
        "Updating command runner flag from {} to {}.",
        this.commandRunnerOption,
        commandRunnerOption);
    this.commandRunnerOption = commandRunnerOption;

    writeToFile();
  }

  /** Getter for the Docker image id. */
  public String getDockerImageId() {
    return dockerImageId;
  }

  /** Setter for the Docker image id. Persists on disk. */
  public void updateDockerImageId(String dockerImageId) {
    logger.info("Updating Docker image id from {} to {}.", this.dockerImageId, dockerImageId);
    this.dockerImageId = dockerImageId;

    writeToFile();
  }

  /** Getter for the resources cache size. */
  public int getResourcesCacheSize() {
    return resourcesCacheSize;
  }

  /**
   * Setter for the resources cache size. Persists on disk.
   *
   * @param resourcesCacheSize new value for the resources cache size
   */
  public void updateResourcesCacheSize(int resourcesCacheSize) {
    logger.info(
        "Updating resources cache size from {} to {}.",
        this.resourcesCacheSize,
        resourcesCacheSize);
    this.resourcesCacheSize = resourcesCacheSize;

    writeToFile();
  }

  /** Getter for the console logging level. */
  public LogLevel getConsoleLoggingLevel() {
    return consoleLoggingLevel;
  }

  /**
   * Setter for the console logging level. Persists on disk.
   *
   * @param consoleLoggingLevel new value for the console logging level
   */
  public void updateConsoleLoggingLevel(LogLevel consoleLoggingLevel) {
    logger.info(
        "Updating console logging level from {} to {}.",
        this.consoleLoggingLevel,
        consoleLoggingLevel);
    this.consoleLoggingLevel = consoleLoggingLevel;

    writeToFile();
  }

  /** Getter for the file logging level. */
  public LogLevel getFileLoggingLevel() {
    return fileLoggingLevel;
  }

  /**
   * Setter for the file logging level. Persists on disk.
   *
   * @param fileLoggingLevel new value for the file logging level
   */
  public void updateFileLoggingLevel(LogLevel fileLoggingLevel) {
    logger.info(
        "Updating file logging level from {} to {}.", this.fileLoggingLevel, fileLoggingLevel);
    this.fileLoggingLevel = fileLoggingLevel;

    writeToFile();
  }

  // ====================================================
  // Directory and file names for persisting on disk
  //   - global context directory parent: $HOME/ or $TERRA_CONTEXT_PARENT_DIR/
  //       - global context directory: .terra/
  //           - persisted global context file: global-context.json
  //           - sub-directory for persisting pet SA keys: pet-keys/[terra user id]/
  //               - pet SA key filename: [workspace id]
  //           - sub-directory for log files: logs/
  //               -*.terra.log
  //           - sub-directory for Java library dependencies: lib/
  //               -*.jar

  /** Write an instance of this class to a JSON-formatted file in the global context directory. */
  public void writeToFile() {
    try {
      JacksonMapper.writeJavaObjectToFile(getGlobalContextFile().toFile(), this);
    } catch (IOException ioEx) {
      logger.error("Error persisting global context.", ioEx);
    }
  }

  /**
   * Get the global context directory.
   *
   * @return absolute path to global context directory
   */
  @JsonIgnore
  public static Path getGlobalContextDir() {
    // default to the user's home directory
    Path parentDir = Paths.get(System.getProperty("user.home"));

    // if the override environment variable is set and points to a valid directory, then use it
    // instead
    String overrideDirName = System.getenv(CONTEXT_DIR_OVERRIDE_NAME);
    if (overrideDirName != null && !overrideDirName.isBlank()) {
      Path overrideDir = Paths.get(overrideDirName).toAbsolutePath();
      if (overrideDir.toFile().exists() && overrideDir.toFile().isDirectory()) {
        parentDir = overrideDir;
      } else {
        throw new UserActionableException(
            "Override environment variable does not point to a valid directory: " + overrideDir);
      }
    }

    return parentDir.resolve(GLOBAL_CONTEXT_DIRNAME).toAbsolutePath();
  }

  /**
   * Get the global context file.
   *
   * @return absolute path to the global context file
   */
  @JsonIgnore
  private static Path getGlobalContextFile() {
    return getGlobalContextDir().resolve(GLOBAL_CONTEXT_FILENAME);
  }

  /**
   * Get the directory that contains the pet SA key files for the given user. This is a
   * sub-directory of the global context directory.
   *
   * @param terraUser user whose key files we want
   * @return absolute path to the key file directory for the given user
   */
  @JsonIgnore
  public static Path getPetSaKeyDir(TerraUser terraUser) {
    return getGlobalContextDir().resolve(PET_KEYS_DIRNAME).resolve(terraUser.getId());
  }

  /**
   * Get the pet SA key file for the current user and workspace. This is stored in a sub-directory
   * of the global context directory.
   *
   * @return absolute path to the pet SA key file for the current user and workspace
   * @throws UserActionableException if the current user or workspace is not defined
   */
  @JsonIgnore
  public Path getPetSaKeyFile() {
    return getPetSaKeyDir(requireCurrentTerraUser())
        .resolve(requireCurrentWorkspace().id.toString());
  }

  /**
   * Get the global log file name.
   *
   * @return absolute path to the log file
   */
  @JsonIgnore
  public static Path getLogFile() {
    return getGlobalContextDir().resolve(LOGS_DIRNAME).resolve(LOG_FILENAME);
  }
}
