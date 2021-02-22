package bio.terra.cli.context;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.ServerManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This POJO class represents an instance of the Terra CLI global context. This is intended
 * primarily for authentication and connection-related context values that will span multiple
 * workspaces.
 */
public class GlobalContext {
  private static final Logger logger = LoggerFactory.getLogger(GlobalContext.class);

  // global auth context =  list of Terra users, CLI-generated key of current Terra user
  public Map<String, TerraUser> terraUsers;
  public String currentTerraUserKey;

  // global server context = service uris, environment name
  public ServerSpecification server;

  // global apps context = docker image id or tag
  public String dockerImageId;

  // file paths related to persisting the global context on disk
  private static final Path DEFAULT_GLOBAL_CONTEXT_DIR =
      Paths.get(System.getProperty("user.home"), ".terra");
  private static final String GLOBAL_CONTEXT_FILENAME = "global-context.json";
  private static final String PET_KEYS_DIRNAME = "pet-keys";

  private GlobalContext() {
    this.terraUsers = new HashMap<>();
    this.server = null;
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
    GlobalContext globalContext = null;
    try {
      globalContext =
          FileUtils.readFileIntoJavaObject(
              resolveGlobalContextFile().toFile(), GlobalContext.class);
    } catch (IOException ioEx) {
      logger.error("Global context file not found.", ioEx);
    }

    // if the global context file does not exist, return an object populated with default values
    if (globalContext == null) {
      globalContext = new GlobalContext();
      globalContext.server = ServerManager.defaultServer();
      globalContext.dockerImageId = DockerAppsRunner.defaultImageId();
    }

    return globalContext;
  }

  /** Write an instance of this class to a JSON-formatted file in the global context directory. */
  private void writeToFile() {
    try {
      FileUtils.writeJavaObjectToFile(resolveGlobalContextFile().toFile(), this);
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

  /** Getter for the global context directory. */
  public static Path resolveGlobalContextDir() {
    // TODO: allow overriding the global context directory path (e.g. env var?)
    return DEFAULT_GLOBAL_CONTEXT_DIR;
  }

  /**
   * Getter for the sub-directory of the global context directory that holds the pet SA key files
   * for all users.
   */
  private static Path resolvePetSaKeyDir() {
    return resolveGlobalContextDir().resolve(PET_KEYS_DIRNAME);
  }

  /**
   * Getter for the sub-directory of the global context directory that holds the pet SA key files
   * for the given user.
   */
  @JsonIgnore
  public static Path getPetSaKeyDirForUser(TerraUser terraUser) {
    return resolvePetSaKeyDir().resolve(terraUser.terraUserId);
  }

  /** Getter for the pet SA key file name for the given user + workspace. */
  @JsonIgnore
  public static String getPetSaKeyFilename(UUID workspaceId) {
    return workspaceId.toString();
  }

  /** Getter for the pet SA key file handle for the given user + workspace. */
  @JsonIgnore
  public static Path getPetSaKeyFile(TerraUser terraUser, UUID workspaceId) {
    return getPetSaKeyDirForUser(terraUser).resolve(getPetSaKeyFilename(workspaceId));
  }

  /** Getter for the file where the global context is persisted. */
  public static Path resolveGlobalContextFile() {
    return resolveGlobalContextDir().resolve(GLOBAL_CONTEXT_FILENAME);
  }
}
