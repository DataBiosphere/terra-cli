package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceNameAndDescription;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFClonedResource;
import bio.terra.cli.serialization.userfacing.UFClonedWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.workspace.model.CloneResourceResult;
import bio.terra.workspace.model.ClonedWorkspace;
import bio.terra.workspace.model.ResourceCloneDetails;
import java.util.Optional;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This corresponds to the third-level "terra workspace clone" command. */
@Command(name = "clone", description = "Clone an existing workspace.")
public class Clone extends BaseCommand {

  @CommandLine.Option(
      names = "--location",
      required = false,
      description = "Location for newly created resources.")
  private String location;

  @CommandLine.Mixin private WorkspaceNameAndDescription workspaceNameAndDescription;

  @CommandLine.Mixin private Format formatOption;

  @CommandLine.Mixin private WorkspaceOverride workspaceOption;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace sourceWorkspace = Context.requireWorkspace();

    ClonedWorkspace clonedWorkspace =
        sourceWorkspace.clone(
            workspaceNameAndDescription.displayName,
            workspaceNameAndDescription.description,
            location);
    Workspace sourceWorkspaceHydrated = Workspace.get(sourceWorkspace.getId());
    Workspace destinationWorkspaceHydrated =
        Workspace.get(clonedWorkspace.getDestinationWorkspaceId());

    // Get a list of UFClonedResource objects based on the resources returned in the ClonedWorkspace
    java.util.List<UFClonedResource> ufClonedResources =
        clonedWorkspace.getResources().stream()
            .map(
                r ->
                    buildUfClonedResource(sourceWorkspaceHydrated, destinationWorkspaceHydrated, r))
            .collect(Collectors.toList());

    // print results
    formatOption.printReturnValue(
        new UFClonedWorkspace(
            new UFWorkspace(sourceWorkspace),
            new UFWorkspace(destinationWorkspaceHydrated),
            ufClonedResources),
        this::printText);
  }

  private UFClonedResource buildUfClonedResource(
      Workspace sourceWorkspace,
      Workspace destinationWorkspace,
      ResourceCloneDetails resourceCloneDetails) {
    Resource sourceResource = sourceWorkspace.getResource(resourceCloneDetails.getName());
    final Resource destinationResource;
    if (CloneResourceResult.SUCCEEDED == resourceCloneDetails.getResult()) {
      destinationResource = destinationWorkspace.getResource(resourceCloneDetails.getName());
    } else {
      destinationResource = null;
    }

    return new UFClonedResource(
        resourceCloneDetails,
        sourceResource.serializeToCommand(),
        Optional.ofNullable(destinationResource).map(Resource::serializeToCommand).orElse(null));
  }

  private void printText(UFClonedWorkspace returnValue) {
    OUT.println("Workspace successfully cloned.");
    returnValue.print();
  }
}
