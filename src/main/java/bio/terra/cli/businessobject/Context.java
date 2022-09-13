package bio.terra.cli.businessobject;

import bio.terra.cli.app.CommandRunner;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.PDContext;
import bio.terra.cli.utils.JacksonMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of the current context or state. This class maintains singleton instances
 * of the internal state classes (Config, Server, User, Workspace).
 */
public class Context {
  // Only exposed for logging in tests
  public static final String LOGS_DIRNAME = "logs";
  private static final Logger logger = LoggerFactory.getLogger(Context.class);
  // env var name to optionally override where the context is persisted on disk
  private static final String CONTEXT_DIR_OVERRIDE_NAME = "TERRA_CONTEXT_PARENT_DIR";
  // file paths related to persisting the context on disk
  private static final String CONTEXT_DIRNAME = ".terra";
  private static final String CONTEXT_FILENAME = "context.json";
  private static final String LOG_FILENAME = "terra.log";
  // singleton objects that define the current context or state
  private static Config currentConfig;
  private static Server currentServer;
  @Nullable private static User currentUser;
  @Nullable private static Workspace currentWorkspace;
  @Nullable private static VersionCheck currentVersionCheck;
  // functions as the current workspace for this command execution only
  // unlike the other parts of the current context, this property is not persisted to disk
  private static Workspace overrideWorkspace;
  // true if the current command is using an override workspace
  private static boolean useOverrideWorkspace;

  /**
   * Reads the context file from disk and initializes the singleton internal state classes (Config,
   * Server, User, Workspace).
   *
   * <p>Note: DO NOT put any logger statements in this function. Because we setup the loggers using
   * the logging levels specified in the context, the loggers have not been setup when we first call
   * this function.
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
      currentVersionCheck =
          diskContext.versionCheck == null ? null : new VersionCheck(diskContext.versionCheck);

    } catch (FileNotFoundException fnfEx) {
      // file not found is a common error here (e.g. first time running the CLI, there will be no
      // pre-existing context file). we handle this by returning an object populated with
      // default values below. so, no need to log or throw the exception returned here.
      logger.debug("Context file not found. Re-initializing with default values");
      initializeDefaults();
    } catch (IOException ioEx) {
      throw new SystemException("Error reading context file from disk.", ioEx);
    }
    overrideWorkspace = null;
    useOverrideWorkspace = false;
  }

  private static void initializeDefaults() {
    currentConfig = new Config();
    currentServer = new Server();
    currentUser = null;
    currentWorkspace = null;
    currentVersionCheck = null;
  }

  /**
   * Writes the current internal state (Config, Server, User, Workspace) to the context file on
   * disk.
   */
  public static void synchronizeToDisk() {
    try {
      PDContext diskContext =
          new PDContext(
              currentConfig, currentServer, currentUser, currentWorkspace, currentVersionCheck);
      JacksonMapper.writeJavaObjectToFile(getContextFile().toFile(), diskContext);
      logger.debug("Wrote context to disk: \n{}", diskContext);
    } catch (IOException ioEx) {
      logger.error("Error persisting context to disk.", ioEx);
    }
  }

  // ====================================================
  // Directory and file names for persisting on disk
  //   - context directory parent: $HOME/ or $TERRA_CONTEXT_PARENT_DIR/
  //       - context directory: .terra/
  //           - persisted context file: context.json
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
    Path contextPath = Paths.get(System.getProperty("user.home"));
    // if the override environment variable is set, use it instead
    String overrideDirName = System.getenv(CONTEXT_DIR_OVERRIDE_NAME);
    if (overrideDirName != null && !overrideDirName.isBlank()) {
      contextPath = Paths.get(overrideDirName);
    }
    // If this is a test, append the current runner's ID. This lets us run multiple tests in
    // parallel without clobbering context across runners.
    String isTest = System.getProperty(CommandRunner.IS_TEST);
    if (isTest != null && isTest.equals("true")) {
      String testWorker = System.getProperty("org.gradle.test.worker");
      if (testWorker != null) {
        contextPath = contextPath.resolve(testWorker);
      }
    }
    // build.gradle test task makes contextDir. However, with test-runner specific directories,
    // this test is executed in a different place from where the test task mkdir was run. So need
    // to create directory for if Test Distribution is being used.
    if (!contextPath.toAbsolutePath().toFile().exists()) {
      contextPath.toAbsolutePath().toFile().mkdir();
    }

    return contextPath.resolve(CONTEXT_DIRNAME).toAbsolutePath();
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

  public static void setUser(User user) {
    currentUser = user;
    synchronizeToDisk();
  }

  public static User requireUser() {
    return getUser()
        .orElseThrow(
            () -> {
              throw new UserActionableException("User not logged in.");
            });
  }

  public static Optional<Workspace> getWorkspace() {
    return Optional.ofNullable(useOverrideWorkspace ? overrideWorkspace : currentWorkspace);
  }

  public static void setWorkspace(Workspace workspace) {
    if (useOverrideWorkspace) {
      overrideWorkspace = workspace;
    } else {
      currentWorkspace = workspace;
      synchronizeToDisk();
    }
  }

  public static Workspace requireWorkspace() {
    return getWorkspace().orElseThrow(() -> new UserActionableException("No workspace set."));
  }

  public static Optional<VersionCheck> getVersionCheck() {
    return Optional.ofNullable(currentVersionCheck);
  }

  public static void setVersionCheck(VersionCheck versionCheck) {
    currentVersionCheck = versionCheck;
    synchronizeToDisk();
  }

  public static void useOverrideWorkspace(String userFacingId) {
    if (currentWorkspace != null && userFacingId.equals(currentWorkspace.getUserFacingId())) {
      // If the user provides the --workspace argument with the same ID as their current workspace,
      // ignore it. We should still update the current context so that the user does not see out
      // of date workspace information.
      return;
    }
    useOverrideWorkspace = true;
    Workspace.load(userFacingId);
  }
}
