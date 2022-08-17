package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferencedResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref git-repo" command. */
@CommandLine.Command(
    name = "git-repo",
    description = "Add a referenced git repository.",
    showDefaultValues = true)
public class GitRepo extends BaseCommand {
  @CommandLine.Mixin ReferencedResourceCreation referencedResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;
  @CommandLine.Option(
      names = "--repo-url",
      required = true,
      description = "URL for cloning the git repository, it can be either HTTPS or SSH urls")
  private String repoUrl;

  /** Print this command's output in text format. */
  private static void printText(UFGitRepo returnValue) {
    OUT.println("Successfully added referenced git repo.");
    returnValue.print();
  }

  /** Add a referenced git repo to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParams =
        referencedResourceCreationOptions.populateMetadataFields();
    AddGitRepoParams.Builder createParams =
        new AddGitRepoParams.Builder()
            .resourceFields(createResourceParams.build())
            .gitRepoUrl(repoUrl);

    bio.terra.cli.businessobject.resource.GitRepo addedResource =
        bio.terra.cli.businessobject.resource.GitRepo.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFGitRepo(addedResource), GitRepo::printText);
  }
}
