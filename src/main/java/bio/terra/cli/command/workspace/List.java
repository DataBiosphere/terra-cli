package bio.terra.cli.command.workspace;

import bio.terra.cli.Context;
import bio.terra.cli.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandWorkspace;
import java.util.Optional;
import java.util.stream.Collectors;
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
    java.util.List<Workspace> workspaces = Workspace.list(offset, limit);
    formatOption.printReturnValue(
        workspaces.stream()
            .map(workspace -> new CommandWorkspace(workspace))
            .collect(Collectors.toList()),
        this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(java.util.List<CommandWorkspace> returnValue) {
    Optional<Workspace> currentWorkspace = Context.getWorkspace();
    for (CommandWorkspace workspace : returnValue) {
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
