package bio.terra.cli.apps;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This class contains logic related to running the supported app nextflow. */
public class Nextflow {
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
    Map<String, String> envVars = new HashMap<>();
    envVars.put("NXF_MODE", "google");

    String fullCommand = DockerAppsRunner.buildFullCommand("nextflow", cmdArgs);
    new DockerAppsRunner(globalContext, workspaceContext).runToolCommand(fullCommand, envVars);
  }
}
