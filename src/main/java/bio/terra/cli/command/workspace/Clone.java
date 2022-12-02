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
import bio.terra.cli.service.UserManagerService;
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
  @CommandLine.Option(names = "--new-id", required = true, description = "ID for new workspace")
  // Variable is `id` instead of `userFacingId` because user sees it with `terra workspace clone`
  private String id;

  @CommandLine.Mixin private WorkspaceNameAndDescription workspaceNameAndDescription;

  @CommandLine.Mixin private Format formatOption;

  @CommandLine.Mixin private WorkspaceOverride workspaceOption;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace sourceWorkspace = Context.requireWorkspace();

    String spendProfile = UserManagerService.fromContext().getDefaultSpendProfile(/*email=*/ null);

    ClonedWorkspace clonedWorkspace =
        sourceWorkspace.clone(
            id,
            workspaceNameAndDescription.name,
            workspaceNameAndDescription.description,
            spendProfile);
    Workspace destinationWorkspaceHydrated =
        Workspace.get(clonedWorkspace.getDestinationWorkspaceId());

    // Get a list of UFClonedResource objects based on the resources returned in the ClonedWorkspace
    java.util.List<UFClonedResource> ufClonedResources =
        clonedWorkspace.getResources().stream()
            .map(r -> buildUfClonedResource(sourceWorkspace, destinationWorkspaceHydrated, r))
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
