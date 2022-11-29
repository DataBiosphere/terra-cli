package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.CloningInstructionsForUpdate;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGitRepoParams;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update git-repo" command. */
@CommandLine.Command(
    name = "git-repo",
    description = "Update a Git Repo.",
    showDefaultValues = true)
public class GitRepo extends WsmBaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin CloningInstructionsForUpdate newCloningInstructionsOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--new-repo-url",
      description = "New git repo url, it can be either https or ssh url.")
  private String newRepoUrl;

  /** Print this command's output in text format. */
  private static void printText(UFGitRepo returnValue) {
    OUT.println("Successfully updated Git repo.");
    returnValue.print();
  }

  /** Update a git repo in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined() && newRepoUrl == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.GitRepo resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.GIT_REPO);

    UpdateReferencedGitRepoParams.Builder gitRepoUpdateParams =
        new UpdateReferencedGitRepoParams.Builder()
            .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
            .gitRepoUrl(newRepoUrl)
            .cloningInstructions(newCloningInstructionsOption.getCloning());
    resource.updateReferenced(gitRepoUpdateParams.build());
    // re-load the resource so we display all properties with up-to-date values
    resource = Context.requireWorkspace().getResource(resource.getName()).castToType(Type.GIT_REPO);
    formatOption.printReturnValue(new UFGitRepo(resource), GitRepo::printText);
  }
}
