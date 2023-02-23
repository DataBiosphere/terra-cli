package bio.terra.cli.command.user.sshkey;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ConfirmationPrompt;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPairType;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra user ssh-key generate" command. */
@Command(name = "generate", description = "Generate a Terra-managed SSH key.")
public class Generate extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--save-to-file",
      description = "Save the Terra SSH key pair as a file, skip printing out the key")
  boolean saveToFile;

  @CommandLine.Mixin ConfirmationPrompt confirmationPrompt;

  @Override
  protected void execute() {
    confirmationPrompt.confirmOrThrow(
        "Generating a new Terra SSH key will replace the old Terra SSH key if it exists. "
            + "You must associate the new SSH public key with your GitHub account using "
            + "https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account "
            + "Are you sure you want to proceed (y/N)?",
        "Generating new SSH key is aborted");
    var sshKeyPair =
        ExternalCredentialsManagerService.fromContext()
            .generateSshKeyPair(SshKeyPairType.GITHUB, saveToFile);
    if (saveToFile) {
      Add.saveKeyFileAndSshAdd(sshKeyPair);
    } else {
      formatOption.printReturnValue(UFSshKeyPair.createUFSshKey(sshKeyPair), UFSshKeyPair::print);
    }
  }
}
