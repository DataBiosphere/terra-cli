package bio.terra.cli.context;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.ServerManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.File;
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

  // file paths related to persisting the global context on disk
  public static final String GLOBAL_CONTEXT_DIRNAME = ".terra";
  private static final String GLOBAL_CONTEXT_FILENAME = "global-context.json";
  private static final String PET_KEYS_DIRNAME = "pet-keys";

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
      return FileUtils.readFileIntoJavaObject(getGlobalContextFileHandle(), GlobalContext.class);
    } catch (IOException ioEx) {
      logger.warn("Global context file not found or error reading it.", ioEx);
    }

    // if the global context file does not exist or there is an error reading it, return an object
    // populated with default values
    return new GlobalContext(ServerManager.defaultServer(), DockerAppsRunner.defaultImageId());
  }

  /** Write an instance of this class to a JSON-formatted file in the global context directory. */
  private void writeToFile() {
    try {
      FileUtils.writeJavaObjectToFile(getGlobalContextFileHandle(), this);
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
      throw new RuntimeException("The current Terra user is not defined. Login required.");
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
    logger.debug(
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
    logger.debug("Updating server from {} to {}.", this.server.name, server.name);
    this.server = server;

    writeToFile();
  }

  // ====================================================
  // Apps

  /** Setter for the Docker image id. Persists on disk. */
  public void updateDockerImageId(String dockerImageId) {
    logger.debug("Updating Docker image id from {} to {}.", this.dockerImageId, dockerImageId);
    this.dockerImageId = dockerImageId;

    writeToFile();
  }

  // ====================================================
  // Directory and file names
  //   - top-level directory: $HOME/.terra
  //       - persisted global context file: global-context.json
  //       - sub-directory for persisting pet SA keys: pet-keys/[terra user id]
  //           - pet SA key filename: [workspace id]

  /**
   * Get the global context directory path relative to the user home directory.
   *
   * @return absolute path to global context directory, relative to the user home directory
   */
  @JsonIgnore
  public static Path getGlobalContextDir() {
    // TODO: allow overriding the global context directory path (e.g. env var != user home
    // directory)
    return getGlobalContextDir(Paths.get(System.getProperty("user.home")));
  }

  /**
   * Get the global context directory path relative to the given directory.
   *
   * @param relativeToDir the top-level directory (e.g. $HOME on either the host or container)
   * @return absolute path to the global context directory, relative to the given directory
   */
  @JsonIgnore
  public static Path getGlobalContextDir(Path relativeToDir) {
    return relativeToDir.resolve(GLOBAL_CONTEXT_DIRNAME).toAbsolutePath();
  }

  /**
   * Get a handle to the global context file.
   *
   * @return handle to the global context file
   */
  @JsonIgnore
  public static File getGlobalContextFileHandle() {
    return getGlobalContextDir().resolve(GLOBAL_CONTEXT_FILENAME).toFile();
  }

  /**
   * Get a handle to the directory that contains the pet SA key files for the given user. This is a
   * sub-directory of the global context directory.
   *
   * @param terraUser user whose key files we want
   * @return handle to the key file directory for the given user
   */
  @JsonIgnore
  public static File getPetSaKeyDirHandle(TerraUser terraUser) {
    return getGlobalContextDir().resolve(PET_KEYS_DIRNAME).resolve(terraUser.terraUserId).toFile();
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
    return getGlobalContextDir()
        .resolve(PET_KEYS_DIRNAME)
        .resolve(terraUser.terraUserId)
        .resolve(workspaceContext.getWorkspaceId().toString());
  }

  /**
   * Get the pet SA key file for the given user and workspace. This is stored in a sub-directory of
   * the global context directory.
   *
   * @param terraUser user whose key file we want
   * @param workspaceContext workspace the key file was created for
   * @param relativeToDir the top-level directory (e.g. $HOME on either the host or container)
   * @return absolute path to the pet SA key file for the given user and workspace
   */
  @JsonIgnore
  public static Path getPetSaKeyFile(
      TerraUser terraUser, WorkspaceContext workspaceContext, Path relativeToDir) {
    return getGlobalContextDir(relativeToDir)
        .resolve(PET_KEYS_DIRNAME)
        .resolve(terraUser.terraUserId)
        .resolve(workspaceContext.getWorkspaceId().toString());
  }
}
