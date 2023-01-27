package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.CloudPlatform;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra nextflow" command. */
@Command(name = "nextflow", description = "Call nextflow in the Terra workspace.")
public class Nextflow extends ToolCommand {
  @Override
  public String getExecutableName() {
    return "nextflow";
  }

  @Override
  public String getVersionArgument() {
    return "-version";
  }

  @Override
  public String getInstallationUrl() {
    return "https://nextflow.io/";
  }

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void executeImpl() {
    workspaceOption.overrideIfSpecified();
    CommandUtils.checkWorkspaceSupport(CloudPlatform.GCP);

    command.add(0, "nextflow");
    Map<String, String> envVars = new HashMap<>();
    envVars.put("NXF_MODE", "google");
    addEnvVarIfDefinedInHost("TOWER_ACCESS_TOKEN", envVars);
    addEnvVarIfDefinedInHost("TOWER_WORKSPACE_ID", envVars);

    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command, envVars);
  }

  private void addEnvVarIfDefinedInHost(String envVarName, Map<String, String> envVars) {
    String envVarValue = System.getenv(envVarName);
    if (envVarValue != null && !envVars.isEmpty()) {
      envVars.put(envVarName, envVarValue);
    }
  }
}
