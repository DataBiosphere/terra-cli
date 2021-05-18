package bio.terra.cli.apps;

import bio.terra.cli.apps.utils.DockerClientWrapper;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs client-side tools in a Docker container and manipulates the tools-related
 * properties of the global context object.
 */
public class DockerCommandRunner extends CommandRunner {
  private static final Logger logger = LoggerFactory.getLogger(DockerCommandRunner.class);

  private final DockerClientWrapper dockerClientWrapper = new DockerClientWrapper();

  // default $HOME directory on the container (this is where we expect to look for the global
  // context)
  private static final String CONTAINER_HOME_DIR = "/root";
  // mount point for the workspace directory
  private static final String CONTAINER_WORKSPACE_DIR = "/usr/local/etc";

  public DockerCommandRunner(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    super(globalContext, workspaceContext);
  }

  /** Returns the default image id. */
  public static String defaultImageId() {
    // read from the JAR Manifest file
    return DockerCommandRunner.class.getPackage().getImplementationVersion();
  }

  /**
   * Update the Docker image property of the global context.
   *
   * <p>Logs a warning if the Docker image is not found locally (i.e. hasn't yet been downloaded
   * with `docker pull`)
   *
   * @param imageId id or tag of the image
   */
  public void updateImageId(String imageId) {
    boolean imageExists = dockerClientWrapper.checkImageExists(imageId);
    if (!imageExists) {
      logger.warn("Image not found: {}", imageId);
    }

    globalContext.updateDockerImageId(imageId);
  }

  /**
   * Run a tool command inside a new Docker container.
   *
   * <p>The terra_init.sh script that was copied into the Docker image will be run before the given
   * command.
   *
   * <p>This method sets the GOOGLE_APPLICATION_CREDENTIALS env var = path to the pet SA key file on
   * the container. This will overwrite any previous version, because the path will likely be
   * different on the container.
   *
   * @param command the command and arguments to execute
   * @param envVars a mapping of environment variable names to values
   */
  protected void runToolCommandImpl(List<String> command, Map<String, String> envVars) {
    // call the terra_init script that was copied into the Docker image, before running the given
    // command
    String fullCommand = "terra_init.sh && " + buildFullCommand(command);

    // set the path to the pet SA key file, which may be different on the container vs the host
    envVars.put(
        "GOOGLE_APPLICATION_CREDENTIALS",
        getPetSaKeyFileOnContainer(globalContext.requireCurrentTerraUser(), workspaceContext)
            .toString());

    // mount the global context directory and the workspace directory to the container
    //  e.g. global context dir (host) $HOME/.terra -> (container) CONTAINER_HOME_DIR/.terra
    //       workspace context dir (host) /Users/mm/workspace123 -> (container) CONTAINER_HOME_DIR
    Map<Path, Path> bindMounts = new HashMap<>();
    bindMounts.put(getGlobalContextDirOnContainer(), GlobalContext.getGlobalContextDir());
    bindMounts.put(Path.of(CONTAINER_WORKSPACE_DIR), WorkspaceContext.getWorkspaceDir());

    // create and start the docker container
    dockerClientWrapper.startContainer(
        globalContext.dockerImageId,
        fullCommand,
        getWorkingDirOnContainer().toString(),
        envVars,
        bindMounts);

    // read the container logs, which contains the command output, and write them to stdout
    dockerClientWrapper.streamLogsForContainer();

    // block until the container exits
    Integer statusCode = dockerClientWrapper.waitForContainerToExit();
    logger.debug("docker run status code: {}", statusCode);

    // delete the container
    dockerClientWrapper.deleteContainer();
  }

  /**
   * Get the working directory on the container so that it matches the current working directory on
   * the host.
   *
   * <p>e.g. working dir on host = /Users/mm/workspace123/nextflow/rnaseq-nf
   *
   * <p>working dir on container = CONTAINER_WORKSPACE_DIR/nextflow/rnaseq-nf
   *
   * @return absolute path to the working directory on the container
   */
  private static Path getWorkingDirOnContainer() {
    // get the current working directory and the workspace directory on the host
    Path currentDir = Paths.get("").toAbsolutePath();
    Path workspaceDirOnHost = WorkspaceContext.getWorkspaceDir();

    // remove the workspace directory part of the current working directory
    Path relativePathToCurrentDir = workspaceDirOnHost.relativize(currentDir);

    // working directory on container = workspace dir on container + relative path to current dir
    return Path.of(CONTAINER_WORKSPACE_DIR).resolve(relativePathToCurrentDir);
  }

  /**
   * Get the global context directory on the container.
   *
   * <p>e.g. (host) $HOME/.terra/ -> (container) CONTAINER_HOME_DIR/.terra/
   *
   * @return absolute path to the global context directory on the container
   */
  private static Path getGlobalContextDirOnContainer() {
    Path globalContextDirName = GlobalContext.getGlobalContextDir().getFileName();
    return Path.of(CONTAINER_HOME_DIR).resolve(globalContextDirName);
  }

  /**
   * Get the pet SA key file for the given user and workspace on the container.
   *
   * <p>e.g. (host) $HOME/.terra/pet-keys/[user id]/[workspace id] -> (container)
   * CONTAINER_HOME_DIR/.terra/pet-keys/[user id]/[workspace id]
   *
   * @return absolute path to the pet SA key file for the given user and workspace on the container
   */
  private static Path getPetSaKeyFileOnContainer(
      TerraUser terraUser, WorkspaceContext workspaceContext) {
    // get the full path of the key file and global context directory on the host
    Path keyFileOnHost = GlobalContext.getPetSaKeyFile(terraUser, workspaceContext);
    Path globalContextDirOnHost = GlobalContext.getGlobalContextDir();

    // remove the global context directory part of the key file path
    // e.g. keyFileOnHost = $HOME/.terra/pet-keys/[user id]/[workspace id]
    //      globalContextDirOnHost = $HOME/.terra/
    //      relativePathToKeyFile = pet-keys/[user id]/[workspace id]
    Path relativePathToKeyFile = globalContextDirOnHost.relativize(keyFileOnHost);

    // key file path on container = global context dir on container + relative path to key file
    return getGlobalContextDirOnContainer().resolve(relativePathToKeyFile);
  }
}
