package bio.terra.cli.context;

import static bio.terra.cli.context.GlobalContext.CommandRunners.DOCKER_CONTAINER;
import static bio.terra.cli.context.utils.Logger.LogLevel;

import bio.terra.cli.apps.CommandRunner;
import bio.terra.cli.apps.DockerCommandRunner;
import bio.terra.cli.apps.LocalProcessCommandRunner;
import bio.terra.cli.auth.AuthenticationManager.BrowserLaunchOption;
import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.utils.JacksonMapper;
import bio.terra.cli.service.ServerManager;
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
public class GlobalContext {
  private static final Logger logger = LoggerFactory.getLogger(GlobalContext.class);

  // global auth context = current Terra user,
  //   flag indicating whether to launch a browser automatically or not
  public TerraUser terraUser;
  public BrowserLaunchOption browserLaunchOption = BrowserLaunchOption.auto;

  // global server context = service uris, environment name
  public ServerSpecification server;

  // global workspaces context = current workspace
  public Workspace workspace;

  // global apps context = flag for how to launch tools, docker image id or tag
  public CommandRunners commandRunnerOption = DOCKER_CONTAINER;
  public String dockerImageId;

  // maximum number of resources to cache on disk for a single workspace before throwing an error
  // (corresponds to ~1MB cache size on disk)
  public int resourcesCacheSize = DEFAULT_RESOURCES_CACHE_SIZE;
  public static final int DEFAULT_RESOURCES_CACHE_SIZE = 1000;

  // global logging context = log levels for file and stdout
  public LogLevel fileLoggingLevel = LogLevel.INFO;
  public LogLevel consoleLoggingLevel = LogLevel.OFF;

  // env var name to optionally override where the context is persisted on disk
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
      globalContext.server = ServerManager.defaultServer();
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
    boolean isNewUser = this.terraUser == null;
    this.terraUser = terraUser;

    // only need to persist the user if there was no user defined previously
    // otherwise, this method just updates the credentials stored on the TerraUser object, which are
    // not persisted to disk
    if (isNewUser) {
      writeToFile();
    }
  }

  /** Clear the current terra user. Persists on disk. */
  public void unsetCurrentTerraUser() {
    this.terraUser = null;
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
   * Setter for the browser launch option. Persists on disk.
   *
   * @param browserLaunchOption new value for the browser launch option
   */
  public void updateBrowserLaunchFlag(BrowserLaunchOption browserLaunchOption) {
    logger.info(
        "Updating browser launch flag from {} to {}.",
        this.browserLaunchOption,
        browserLaunchOption);
    this.browserLaunchOption = browserLaunchOption;

    writeToFile();
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

  /** Setter for the current Terra server. Persists on disk. */
  public void updateServer(ServerSpecification server) {
    logger.info("Updating server from {} to {}.", this.server.name, server.name);
    this.server = server;

    writeToFile();
  }

  /** Setter for the Docker image id. Persists on disk. */
  public void updateDockerImageId(String dockerImageId) {
    logger.info("Updating Docker image id from {} to {}.", this.dockerImageId, dockerImageId);
    this.dockerImageId = dockerImageId;

    writeToFile();
  }

  // TODO (mariko): move this to CommandRunner
  /** This enum defines the different ways of running tool/app commands. */
  public enum CommandRunners {
    DOCKER_CONTAINER,
    LOCAL_PROCESS;
  }

  /** Helper method to get the {@link CommandRunner} sub-class that maps to each enum value. */
  public CommandRunner getRunner(WorkspaceContext workspaceContext) {
    switch (commandRunnerOption) {
      case DOCKER_CONTAINER:
        return new DockerCommandRunner(this, workspaceContext);
      case LOCAL_PROCESS:
        return new LocalProcessCommandRunner(this, workspaceContext);
      default:
        throw new SystemException("Unsupported command runner type: " + this);
    }
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

  /** Write an instance of this class to a JSON-formatted file in the global context directory. */
  private void writeToFile() {
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
    return getGlobalContextDir().resolve(PET_KEYS_DIRNAME).resolve(terraUser.terraUserId);
  }

  /**
   * Get the pet SA key file for the given user and workspace. This is stored in a sub-directory of
   * the global context directory.
   *
   * @param terraUser user whose key file we want
   * @param workspaceContext workspace the key file was created for
   * @return absolute path to the pet SA key file for the given user and workspace
   */
  @JsonIgnore
  public static Path getPetSaKeyFile(TerraUser terraUser, WorkspaceContext workspaceContext) {
    return getPetSaKeyDir(terraUser).resolve(workspaceContext.getWorkspaceId().toString());
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
