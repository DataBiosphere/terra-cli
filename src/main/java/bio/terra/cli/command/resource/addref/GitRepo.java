package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferencedResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPairType;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
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
    ExternalCredentialsManagerService ecmService = ExternalCredentialsManagerService.fromContext();
    try {
      ecmService.getSshKeyPair(SshKeyPairType.GITHUB);
    } catch (SystemException e) {
      if (e.getCause() instanceof HttpStatusCodeException
          && ((HttpStatusCodeException) e.getCause()).getStatusCode() == HttpStatus.NOT_FOUND) {
        throw new UserActionableException(
            "You do not have a Terra ssh key, cloning the git repo in the GCP notebook will"
                + " fail. Please run `terra user ssh-key generate` and store the output (public key) in"
                + " your GitHub account https://github.com/settings/keys.");
      }
    }
  }
}
