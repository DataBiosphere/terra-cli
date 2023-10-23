package bio.terra.cli.command;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.CloudPlatform;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra snapshot" command. */
@Command(
    name = "snapshot",
    description = "Creates a snapshot of a file and places it in a `snapshots` bucket.")
public class Snapshot extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

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
  public String outputBucket;

  @Override
  protected void execute() {
    // 1) If the output bucket doesn't exist, create it
    // 2) gsutil cp the filepath to the output bucket
    // 3) Echo the comment to a temp local file
    // 4) gsutil cp the comment file to the output bucket
    // 5) Cleanup the temp local file

    workspaceOption.overrideIfSpecified();
    CommandUtils.checkWorkspaceSupport(CloudPlatform.GCP);

    // 1) If the output bucket doesn't exist, create it
    String googleProjectId = Context.requireWorkspace().getRequiredGoogleProjectId();
    String petSaEmail = Context.requireUser().getPetSaEmail();
    OUT.println(Context.requireUser().getEmail());
    String bucketCloudId;
    try {
      Resource resource = Context.requireWorkspace().getResource(outputBucket);
      bucketCloudId = ((GcsBucket) resource).resolve();
      OUT.println(bucketCloudId);

    } catch (UserActionableException e) {
      OUT.println("Caught Exception");
      throw e;
      // TODO: Auto create bucket
      // Current difficulties auto-creating bucket:
      // - How do I pass in my own parameters? Currently the CreateGcsBucketParams.Builder
      //   takes in a CreateResourceParams, which takes in a ControlledResourceCreation mixin,
      //   which adds options like --name and --description to the snapshot command
    }
    OUT.println("Willy2");

    // 2) gsutil cp the filepath to the output bucket
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuuMMdd");
    String dateString = dtf.format(LocalDate.now());
    List<String> commands = new ArrayList<>();
    String fullGcsOutputPath =
        bucketCloudId
            + "/notebook_snapshots/"
            + Context.requireUser().getEmail()
            + "/"
            + dateString
            + "/";
    OUT.println("bucketCloudId");
    commands.add("gsutil cp " + filePath + " " + bucketCloudId);

    // Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(commands);

    // 3) Echo the comment to a temp local file

    // 4) gsutil cp the comment file to the output bucket
    // 5) Cleanup the temp local file
    return;
  }
}
