package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferenceCreation;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref git-repo" command. */
@CommandLine.Command(
    name = "git-repo",
    description = "Add a referenced git repository.",
    showDefaultValues = true)
public class GitRepo extends BaseCommand {
  @CommandLine.Mixin ReferenceCreation referenceCreationOptions;

  @CommandLine.Option(
      names = "--repo-url",
      required = true,
      description = "URL for cloning the git repository, it can be either HTTPS or SSH urls")
  private String repoUrl;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a referenced git repo to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParams =
        referenceCreationOptions
            .populateMetadataFields();
    AddGitRepoParams.Builder createParams =
        new AddGitRepoParams.Builder()
            .resourceFields(createResourceParams.build())
            .gitRepoUrl(repoUrl);

    bio.terra.cli.businessobject.resource.GitRepo addedResource =
        bio.terra.cli.businessobject.resource.GitRepo.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFGitRepo(addedResource), GitRepo::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGitRepo returnValue) {
    OUT.println("Successfully added referenced git repo.");
    returnValue.print();
  }
}
