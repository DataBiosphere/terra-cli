package bio.terra.cli.command.workspace;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "clone", description = "Clone an existing workspace.")
public class Clone extends BaseCommand {

  @CommandLine.Option(names = "--id", required = true, description = "Workspace ID to clone.")
  private String workspaceId;

  @CommandLine.Option(
      names = "--spendProfile",
      required = true,
      description = "Spend Profile ID for the workspace created by cloning.")
  private String spendProfile;

  @CommandLine.Option(
      names = "--location",
      required = false,
      description = "Location for newly created resources.")
  private String location;

  @CommandLine.Option(
      names="--name",
      required = false,
      description = "Display name for new workspace."
  )
  private String name;

  @CommandLine.Option(
      names="--description",
      required = false,
      description = "Workspace description."
  )
  private String description;

  @CommandLine.Mixin Format formatOption;

  @Override
  protected void execute() {
  }
}
