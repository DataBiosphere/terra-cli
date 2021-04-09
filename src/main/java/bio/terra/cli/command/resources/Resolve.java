package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources resolve" command. */
@CommandLine.Command(name = "resolve", description = "Resolve a resource to its cloud id or path.")
public class Resolve extends BaseCommand {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the resource, scoped to the workspace.")
  private String name;

  @CommandLine.Mixin FormatOption formatOption;

  /** Resolve a resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    CloudResource resource =
        new WorkspaceManager(globalContext, workspaceContext).getControlledResource(name);
    formatOption.printReturnValue(resource.cloudId);
  }
}
