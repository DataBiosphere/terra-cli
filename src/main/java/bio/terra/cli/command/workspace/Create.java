package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceNameAndDescription;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.CloudPlatform;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace create" command. */
@Command(name = "create", description = "Create a new workspace.")
public class Create extends WsmBaseCommand {
  @CommandLine.Mixin WorkspaceNameAndDescription workspaceNameAndDescription;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--properties",
      required = false,
      split = ",",
      description =
          "Workspace properties. Example: --properties=key=value. For multiple properties, use \",\": --properties=key1=value1,key2=value2")
  public Map<String, String> workspaceProperties;

  @CommandLine.Option(names = "--id", required = true, description = "Workspace ID")
  // Variable is `id` instead of `userFacingId` because user sees it with `terra workspace create`
  private String id;

  @CommandLine.Option(
      names = "--platform",
      description = "Set the Cloud platform: ${COMPLETION-CANDIDATES}.",
      defaultValue = "GCP",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  private CloudPlatform cloudPlatform;

  /** Create a new workspace. */
  @Override
  protected void execute() {
    CommandUtils.checkPlatformSupport(cloudPlatform);

    Workspace workspace =
        Workspace.create(
            id,
            cloudPlatform,
            workspaceNameAndDescription.name,
            workspaceNameAndDescription.description,
            workspaceProperties);
    formatOption.printReturnValue(new UFWorkspace(workspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace successfully created.");
    returnValue.print();
  }
}
