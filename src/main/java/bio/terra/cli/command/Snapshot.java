package bio.terra.cli.command;

import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra snapshot" command. */
@Command(
    name = "snapshot",
    description = "Creates a snapshot of a file and places it in a `snapshots` bucket.")
public class Snapshot extends BaseCommand {

  @CommandLine.Option(
      names = "--filePath",
      required = true,
      description = "The path to a file to create a snapshot of.")
  public String filePath;

  @CommandLine.Option(
      names = "--comment",
      required = true,
      description = "A \"commit\" message for the snapshot.")
  public String comment;

  @CommandLine.Option(
      names = "--output",
      defaultValue = "snapshots",
      required = false,
      description =
          "The name of a Terra resource to place the resulting snapshot. Defaults to `snapshots`. If the resource doesn't exist, create it.")
  public String output;

  @Override
  protected void execute() {
    // 1) If the output bucket doesn't exist, create it
    // 2) gsutil cp the filepath to the output bucket
    // 3) Echo the comment to a temp local file
    // 4) gsutil cp the comment file to the output bucket
    // 5) Cleanup the temp local file
    return;
  }
}
