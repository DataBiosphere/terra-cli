package bio.terra.cli.context;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.command.exception.UserFacingException;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.ServerManager;
import ch.qos.logback.classic.Level;
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
  public boolean launchBrowserAutomatically = true;

  // global server context = service uris, environment name
  public ServerSpecification server;

  // global apps context = docker image id or tag
  public String dockerImageId;

  // TODO (PF-542): add a config command to allow modifying these levels without re-compiling
  // global logging context = log levels for file and stdout
  public Level fileLoggingLevel = Level.DEBUG;
  public Level consoleLoggingLevel = Level.OFF;

  // file paths related to persisting the global context on disk
  private static final String GLOBAL_CONTEXT_DIRNAME = ".terra";
  private static final String GLOBAL_CONTEXT_FILENAME = "global-context.json";
  private static final String PET_KEYS_DIRNAME = "pet-keys";
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
   * @return an instance of this class
   */
  public static GlobalContext readFromFile() {
    // try to read in an instance of the global context file
    try {
      return FileUtils.readFileIntoJavaObject(getGlobalContextFile().toFile(), GlobalContext.class);
    } catch (IOException ioEx) {
      logger.debug("Global context file not found or error reading it.", ioEx);
    }

    // if the global context file does not exist or there is an error reading it, return an object
    // populated with default values
    return new GlobalContext(ServerManager.defaultServer(), DockerAppsRunner.defaultImageId());
  }

  /** Write an instance of this class to a JSON-formatted file in the global context directory. */
  private void writeToFile() {
    try {
      FileUtils.writeJavaObjectToFile(getGlobalContextFile().toFile(), this);
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
      throw new UserFacingException("The current Terra user is not defined. Login required.");
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

  /** Setter for the browser launch flag. Persists on disk. */
  public void updateBrowserLaunchFlag(boolean launchBrowserAutomatically) {
    logger.info(
        "Updating browser launch flag from {} to {}.",
        this.launchBrowserAutomatically,
        launchBrowserAutomatically);
    this.launchBrowserAutomatically = launchBrowserAutomatically;

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

  // ====================================================
  // Directory and file names
  //   - global context directory parent: $HOME/
  //       - global context directory: .terra/
  //           - persisted global context file: global-context.json
  //           - global log file: terra.log
  //           - sub-directory for persisting pet SA keys: pet-keys/[terra user id]/
  //               - pet SA key filename: [workspace id]

  /**
   * Get the global context directory.
   *
   * @return absolute path to global context directory
   */
  @JsonIgnore
  public static Path getGlobalContextDir() {
    // TODO: allow overriding this (e.g. env var != user home directory)
    Path parentDir = Paths.get(System.getProperty("user.home"));
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
    return getGlobalContextDir().resolve(LOG_FILENAME);
  }
}
