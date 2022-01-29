package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.PDContext;
import bio.terra.cli.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of the current context or state. This class maintains singleton instances
 * of the internal state classes (Config, Server, User, Workspace).
 */
public class Context {
  private static final Logger logger = LoggerFactory.getLogger(Context.class);

  // singleton objects that define the current context or state
  private static Config currentConfig;
  private static Server currentServer;
  private static User currentUser;
  private static Workspace currentWorkspace;

  // functions as the current workspace for this command execution only
  // unlike the other parts of the current context, this property is not persisted to disk
  private static Workspace overrideWorkspace;
  // true if the current command is using an override workspace
  private static boolean useOverrideWorkspace;

  // env var name to optionally override where the context is persisted on disk
  private static final String CONTEXT_DIR_OVERRIDE_NAME = "TERRA_CONTEXT_PARENT_DIR";

  // file paths related to persisting the context on disk
  private static final String CONTEXT_DIRNAME = ".terra";
  private static final String CONTEXT_FILENAME = "context.json";
  private static final String PET_KEYS_DIRNAME = "pet-keys";
  private static final String LOGS_DIRNAME = "logs";
  private static final String LOG_FILENAME = "terra.log";

  /**
   * Reads the context file from disk and initializes the singleton internal state classes (Config,
   * Server, User, Workspace).
   *
   * <p>Note: DO NOT put any logger statements in this function. Because we setup the loggers using
   * the logging levels specified in the global context, the loggers have not been setup when we
   * first call this function.
   */
  public static void initializeFromDisk() {
    try {
      // try to read in an instance of the context file
      PDContext diskContext =
          JacksonMapper.readFileIntoJavaObject(getContextFile().toFile(), PDContext.class);
      currentConfig = new Config(diskContext.config);
      currentServer = new Server(diskContext.server);
      currentUser = diskContext.user == null ? null : new User(diskContext.user);
      currentWorkspace =
          diskContext.workspace == null ? null : new Workspace(diskContext.workspace);
    } catch (IOException ioEx) {
      System.out.println("yuhuyoyo io exception from syncing to disk");
      // file not found is a common error here (e.g. first time running the CLI, there will be no
      // pre-existing global context file). we handle this by returning an object populated with
      // default values below. so, no need to log or throw the exception returned here.
      currentConfig = new Config();
      currentServer = new Server();
      currentUser = null;
      currentWorkspace = null;
    }
    overrideWorkspace = null;
    useOverrideWorkspace = false;
  }

  /**
   * Writes the current internal state (Config, Server, User, Workspace) to the context file on
   * disk.
   */
  public static void synchronizeToDisk() {
    try {
      if (currentWorkspace == null) {
        System.out.println("current workspace id is null");
      } else {
        System.out.println("synchornize to disk, " + currentWorkspace.getGoogleProjectId());
      }
      PDContext diskContext =
          new PDContext(currentConfig, currentServer, currentUser, currentWorkspace);
      JacksonMapper.writeJavaObjectToFile(getContextFile().toFile(), diskContext);
    } catch (IOException ioEx) {
      logger.error("Error persisting context to disk.", ioEx);
    }
  }

  // ====================================================
  // Directory and file names for persisting on disk
  //   - context directory parent: $HOME/ or $TERRA_CONTEXT_PARENT_DIR/
  //       - context directory: .terra/
  //           - persisted context file: global-context.json
  //           - sub-directory for persisting pet SA keys: pet-keys/[terra user id]/
  //               - pet SA key filename: [workspace id]
  //           - sub-directory for log files: logs/
  //               -*.terra.log
  //           - sub-directory for Java library dependencies: lib/
  //               -*.jar

  /**
   * Get the context directory.
   *
   * @return absolute path to context directory
   */
  public static Path getContextDir() {
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

    return parentDir.resolve(CONTEXT_DIRNAME).toAbsolutePath();
  }

  /**
   * Get the context file.
   *
   * @return absolute path to the context file
   */
  public static Path getContextFile() {
    return getContextDir().resolve(CONTEXT_FILENAME);
  }

  /**
   * Get the directory that contains the pet SA key files for the given user. This is a
   * sub-directory of the context directory.
   *
   * @param user user whose key files we want
   * @return absolute path to the key file directory for the given user
   */
  public static Path getPetSaKeyDir(User user) {
    return getContextDir().resolve(PET_KEYS_DIRNAME).resolve(user.getId());
  }

  /**
   * Get the pet SA key file for the current user and workspace. This is stored in a sub-directory
   * of the global context directory.
   *
   * @return absolute path to the pet SA key file for the current user and workspace
   * @throws UserActionableException if the current user or workspace is not defined
   */
  public static Path getPetSaKeyFile() {
    return getPetSaKeyFile(requireUser());
  }

  /**
   * Get the pet SA key file for a user and the current workspace. This is stored in a sub-directory
   * of the global context directory.
   *
   * @param user
   * @return absolute path to the pet SA key file for the given user and current workspace
   * @throws UserActionableException if the current workspace is not defined
   */
  public static Path getPetSaKeyFile(User user) {
    return Context.getPetSaKeyDir(user).resolve(requireWorkspace().getId().toString());
  }

  /**
   * Get the log file name.
   *
   * @return absolute path to the log file
   */
  public static Path getLogFile() {
    return getContextDir().resolve(LOGS_DIRNAME).resolve(LOG_FILENAME);
  }

  // ====================================================
  // Singleton get/setters.
  public static Config getConfig() {
    if (currentConfig == null) {
      throw new SystemException("Config not initialized.");
    }
    return currentConfig;
  }

  public static Server getServer() {
    if (currentServer == null) {
      throw new SystemException("Server not initialized.");
    }
    return currentServer;
  }

  public static void setServer(Server server) {
    currentServer = server;
    synchronizeToDisk();
  }

  public static Optional<User> getUser() {
    return Optional.ofNullable(currentUser);
  }

  public static User requireUser() {
    return getUser()
        .orElseThrow(
            () -> {
              throw new UserActionableException("User not logged in.");
            });
  }

  public static void setUser(User user) {
    currentUser = user;
    synchronizeToDisk();
  }

  public static Optional<Workspace> getWorkspace() {
    System.out.println(
        "yuhuyoyo useOverrideWorkspace"
            + useOverrideWorkspace
            + " current workspace: "
            + currentWorkspace);
    return Optional.ofNullable(useOverrideWorkspace ? overrideWorkspace : currentWorkspace);
  }

  public static Workspace requireWorkspace() {
    return getWorkspace()
        .orElseThrow(
            () -> {
              throw new UserActionableException("No workspace set.");
            });
  }

  public static void setWorkspace(Workspace workspace) {
    System.out.println("yuhuyoyo set workspace");
    if (useOverrideWorkspace) {
      overrideWorkspace = workspace;
    } else {
      currentWorkspace = workspace;
      synchronizeToDisk();
    }
  }

  public static void useOverrideWorkspace(UUID id) {
    if (currentWorkspace != null && id.equals(currentWorkspace.getId())) {
      // If the user provides the --workspace argument with the same ID as their current workspace,
      // ignore it. We should still update the current context so that the user does not see out
      // of date workspace information.
      return;
    }
    useOverrideWorkspace = true;
    Workspace.load(id);
  }
}
