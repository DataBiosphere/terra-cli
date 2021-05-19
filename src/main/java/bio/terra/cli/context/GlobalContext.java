package bio.terra.cli.context;

import static bio.terra.cli.context.GlobalContext.CommandRunners.DOCKER_CONTAINER;
import static bio.terra.cli.context.GlobalContext.CommandRunners.LOCAL_PROCESS;
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
import java.util.HashMap;
import java.util.Map;
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

  // global auth context =  list of Terra users, CLI-generated key of current Terra user,
  //   flag indicating whether to launch a browser automatically or not
  public Map<String, TerraUser> terraUsers;
  public String currentTerraUserKey;
  public BrowserLaunchOption browserLaunchOption = BrowserLaunchOption.auto;

  // global server context = service uris, environment name
  public ServerSpecification server;

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
  private static final String CONTEXT_DIR_OVERRIDE_NAME = "TERRA_CONTEXT_DIR";

  // file paths related to persisting the global context on disk
  private static final String GLOBAL_CONTEXT_DIRNAME = ".terra";
  private static final String GLOBAL_CONTEXT_FILENAME = "global-context.json";
  private static final String PET_KEYS_DIRNAME = "pet-keys";
  private static final String LOGS_DIRNAME = "logs";
  private static final String LOG_FILENAME = "terra.log";

  // defaut constructor needed for Jackson de/serialization
  private GlobalContext() {}

  private GlobalContext(ServerSpecification server, String dockerImageId) {
    this.terraUsers = new HashMap<>();
    this.server = server;
    this.dockerImageId = dockerImageId;
  }

  // ====================================================
  // Persisting on disk

  /**
   * Read in an instance of this class from a JSON-formatted file in the global context directory.
   * If there is no existing file, this method returns an object populated with default values.
   *
   * <p>Note: DO NOT put any logger statements in this function. Because we setup the loggers using
   * the logging levels specified in the global context, the loggers have not been setup when we
   * first call this function.
   *
   * @return an instance of this class
   */
  public static GlobalContext readFromFile() {
    // try to read in an instance of the global context file
    try {
      return JacksonMapper.readFileIntoJavaObject(
          getGlobalContextFile().toFile(), GlobalContext.class);
    } catch (IOException ioEx) {
      // file not found is a common error here (e.g. first time running the CLI, there will be no
      // pre-existing global context file). we handle this by returning an object populated with
      // default values below. so, no need to log or throw the exception returned here.
    }

    // if the global context file does not exist or there is an error reading it, return an object
    // populated with default values
    return new GlobalContext(ServerManager.defaultServer(), DockerCommandRunner.defaultImageId());
  }

  /** Write an instance of this class to a JSON-formatted file in the global context directory. */
  private void writeToFile() {
    try {
      JacksonMapper.writeJavaObjectToFile(getGlobalContextFile().toFile(), this);
    } catch (IOException ioEx) {
      logger.error("Error persisting global context.", ioEx);
    }
  }

  // ====================================================
  // Auth

  /** Getter for the current Terra user. Returns null if no current user is defined. */
  @JsonIgnore
  public Optional<TerraUser> getCurrentTerraUser() {
    if (currentTerraUserKey == null) {
      return Optional.empty();
    }
    return Optional.of(terraUsers.get(currentTerraUserKey));
  }

  /** Utility method that throws an exception if the current Terra user is not defined. */
  public TerraUser requireCurrentTerraUser() {
    Optional<TerraUser> terraUserOpt = getCurrentTerraUser();
    if (!terraUserOpt.isPresent()) {
      throw new UserActionableException("The current Terra user is not defined. Login required.");
    }
    return terraUserOpt.get();
  }

  /**
   * Add a new Terra user to the list of identity contexts, or update an existing one. Optionally
   * update the current user. Persists on disk.
   */
  public void addOrUpdateTerraUser(TerraUser terraUser, boolean setAsCurrentUser) {
    if (terraUsers.get(terraUser.cliGeneratedUserKey) != null) {
      logger.info("Terra user {} already exists, updating.", terraUser.terraUserEmail);
    }
    terraUsers.put(terraUser.cliGeneratedUserKey, terraUser);

    if (setAsCurrentUser) {
      currentTerraUserKey = terraUser.cliGeneratedUserKey;
    }

    writeToFile();
  }

  /**
   * Add a new Terra user to the list of identity contexts, or update an existing one. Persists on
   * disk.
   */
  public void addOrUpdateTerraUser(TerraUser terraUser) {
    addOrUpdateTerraUser(terraUser, false);
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

  // ====================================================
  // Server

  /** Setter for the current Terra server. Persists on disk. */
  public void updateServer(ServerSpecification server) {
    logger.info("Updating server from {} to {}.", this.server.name, server.name);
    this.server = server;

    writeToFile();
  }

  // ====================================================
  // Apps

  /** Setter for the Docker image id. Persists on disk. */
  public void updateDockerImageId(String dockerImageId) {
    logger.info("Updating Docker image id from {} to {}.", this.dockerImageId, dockerImageId);
    this.dockerImageId = dockerImageId;

    writeToFile();
  }

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
  // Directory and file names
  //   - global context directory parent: $HOME/ or $TERRA_CONTEXT_DIR/
  //       - global context directory: .terra/
  //           - persisted global context file: global-context.json
  //           - sub-directory for persisting pet SA keys: pet-keys/[terra user id]/
  //               - pet SA key filename: [workspace id]
  //           - sub-directory for log files: logs/
  //               -*.terra.log

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
        throw new SystemException(
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
