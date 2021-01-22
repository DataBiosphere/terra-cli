package bio.terra.cli.model;

import bio.terra.cli.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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
          FileUtils.readOutputFileIntoJavaObject(
              resolveGlobalContextFile().toFile(), GlobalContext.class);
    } catch (IOException ioEx) {
      logger.error("Error reading in global context file.", ioEx);
    }

    // if the global context file does not exist, return an object populated with default values
    if (globalContext == null)
      try {
        globalContext = new GlobalContext();
        globalContext.server = ServerSpecification.fromJSONFile(DEFAULT_SERVER_FILENAME);
      } catch (IOException ioEx) {
        logger.error("Error reading in default server file. ({})", DEFAULT_SERVER_FILENAME, ioEx);
      }

    return globalContext;
  }

  /** Write an instance of this class to a JSON-formatted file in the global context directory. */
  public void writeToFile() {
    try {
      FileUtils.writeJavaObjectToFile(resolveGlobalContextFile().toFile(), this);
    } catch (IOException ioEx) {
      logger.error("Error persisting global context.", ioEx);
    }
  }

  // ====================================================
  // Auth

  /** Getter for the current Terra user. Returns null if no current user is defined. */
  public TerraUser getCurrentTerraUser() {
    if (currentTerraUserKey == null) {
      return null;
    }
    return terraUsers.get(currentTerraUserKey);
  }

  /** Setter for the current Terra user. */
  public void setCurrentTerraUser(TerraUser currentTerraUser) {
    if (terraUsers.get(currentTerraUser.cliGeneratedUserKey) == null) {
      throw new RuntimeException(
          "Terra user "
              + currentTerraUser.terraUserName
              + " not found in the list of known identity contexts.");
    }
    currentTerraUserKey = currentTerraUser.cliGeneratedUserKey;
  }

  /** Add a new Terra user to the list of identity contexts, or update an existing one. */
  public void addOrUpdateTerraUser(TerraUser terraUser) {
    if (terraUsers.get(terraUser.cliGeneratedUserKey) != null) {
      logger.debug("Terra user {} already exists, updating.", terraUser.terraUserName);
    }
    terraUsers.put(terraUser.cliGeneratedUserKey, terraUser);
  }

  // ====================================================
  // Server

  // This variable defines the server that the CLI points to by default.
  private static final String DEFAULT_SERVER_FILENAME = "terra-dev.json";

  // ====================================================
  // Directory and file names
  //   - top-level directory: $HOME/.terra-cli
  //   - persisted global context file: global_context.json
  //   - sub-directory for persisting pet SA keys: pet_SA_keys

  private static final Path DEFAULT_GLOBAL_CONTEXT_DIR =
      Paths.get(System.getProperty("user.home"), ".terra-cli");
  private static final String GLOBAL_CONTEXT_FILENAME = "global_context.json";
  private static final String PET_SA_KEYS_DIRNAME = "pet_SA_keys";

  /** Getter for the global context directory. */
  public static Path resolveGlobalContextDir() {
    // TODO: allow overriding the global context directory path (e.g. env var?)
    return DEFAULT_GLOBAL_CONTEXT_DIR;
  }

  /**
   * Getter for the sub-directory of the global context directory that holds the pet SA key files.
   */
  public static Path resolvePetSAKeyDir() {
    return resolveGlobalContextDir().resolve(PET_SA_KEYS_DIRNAME);
  }

  /** Getter for the file where the global context is persisted. */
  public static Path resolveGlobalContextFile() {
    return resolveGlobalContextDir().resolve(GLOBAL_CONTEXT_FILENAME);
  }
}
