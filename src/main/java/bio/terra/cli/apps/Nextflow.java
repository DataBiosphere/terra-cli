package bio.terra.cli.apps;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class contains logic related to running the supported app nextflow. */
public class Nextflow {
  private static final Logger logger = LoggerFactory.getLogger(Nextflow.class);

  // Mount point for the nextflow sub-directory on the Docker container.
  public static final String NEXTFLOW_MOUNT_POINT = "/usr/local/etc/nextflow";

  // Default nextflow sub-directory relative to the directory where the Terra CLI is running.
  // TODO: walk up the directory tree to find this directory relative to the top-level workspace
  // dir, not the current dir
  private static final Path DEFAULT_NEXTFLOW_DIR = Paths.get(".", "nextflow");

  private final GlobalContext globalContext;
  private final WorkspaceContext workspaceContext;

  /** Set the global and workspace context properties. */
  public Nextflow(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
  }

  /**
   * Run nextflow in a Docker container.
   *
   * @param cmdArgs command arguments to pass through to nextflow
   */
  public void run(List<String> cmdArgs) {
    // mount the nextflow sub-directory of the current directory
    createSubDirectory();
    Map<String, File> bindMounts = new HashMap<>();
    bindMounts.put(NEXTFLOW_MOUNT_POINT, DEFAULT_NEXTFLOW_DIR.toFile());

    String fullCommand = DockerAppsRunner.buildFullCommand("nextflow", cmdArgs);
    new DockerAppsRunner(globalContext, workspaceContext)
        .runToolCommand(fullCommand, NEXTFLOW_MOUNT_POINT, new HashMap<>(), bindMounts);
  }

  /** Create the nextflow sub-directory on the host machine if it does not already exist. */
  private void createSubDirectory() {
    File nextflowDir = DEFAULT_NEXTFLOW_DIR.toFile();
    if (!nextflowDir.exists() || !nextflowDir.isDirectory()) {
      boolean nextflowDirCreated = nextflowDir.mkdirs();
      if (!nextflowDirCreated) {
        throw new RuntimeException("Error creating nextflow sub-directory.");
      }
    }
  }
}
