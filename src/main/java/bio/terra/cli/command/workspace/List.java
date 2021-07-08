package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list" command. */
@Command(
    name = "list",
    description = "List all workspaces the current user can access.",
    showDefaultValues = true)
public class List extends BaseCommand {

  @CommandLine.Option(
      names = "--offset",
      required = false,
      defaultValue = "0",
      description =
          "The offset to use when listing workspaces. (Zero means to start from the beginning.)")
  private int offset;

  @CommandLine.Option(
      names = "--limit",
      required = false,
      defaultValue = "30",
      description = "The maximum number of workspaces to return.")
  private int limit;

  @CommandLine.Mixin Format formatOption;

  /** List all workspaces a user has access to. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        UserIO.sortAndMap(
            Workspace.list(offset, limit),
            Comparator.comparing(Workspace::getName),
            UFWorkspace::new),
        this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(java.util.List<UFWorkspace> returnValue) {
    Optional<Workspace> currentWorkspace = Context.getWorkspace();
    for (UFWorkspace workspace : returnValue) {
      String prefix =
          (currentWorkspace.isPresent() && currentWorkspace.get().getId().equals(workspace.id))
              ? " * "
              : "   ";
      OUT.println(prefix + workspace.id);

      String propertyDescription = "%16s: %s";
      String displayName = workspace.name;
      if (!(displayName == null || displayName.isBlank())) {
        OUT.println(String.format(propertyDescription, "Name", displayName));
      }
      String description = workspace.description;
      if (!(description == null || description.isBlank())) {
        OUT.println(String.format(propertyDescription, "Description", description));
      }
    }
  }
}
