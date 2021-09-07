package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFClonedWorkspace;
import bio.terra.workspace.model.ClonedWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "clone", description = "Clone an existing workspace.")
public class Clone extends BaseCommand {

  @CommandLine.Option(
      names = "--location",
      required = false,
      description = "Location for newly created resources.")
  private String location;

  @CommandLine.Option(
      names = "--name",
      required = false,
      description = "Display name for new workspace.")
  private String name;

  @CommandLine.Option(
      names = "--description",
      required = false,
      description = "Workspace description.")
  private String description;

  @CommandLine.Mixin Format formatOption;

  @Override
  protected void execute() {
    Workspace workspaceToClone = Context.requireWorkspace();
    ClonedWorkspace clonedWorkspace = workspaceToClone.clone(name, description, location);
    // print results
    formatOption.printReturnValue(new UFClonedWorkspace(clonedWorkspace), this::printText);
  }

  private void printText(UFClonedWorkspace returnValue) {
    OUT.println("Workspace successfully cloned.");
    returnValue.print();
  }
}
