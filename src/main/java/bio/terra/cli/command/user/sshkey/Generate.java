package bio.terra.cli.command.user.sshkey;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ConfirmationPrompt;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPairType;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra user ssh-key generate" command. */
@Command(name = "generate", description = "Generate a terra managed ssh key.")
public class Generate extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--save-to-file",
      description = "Save the terra ssh key pair as file, skip printing out the key")
  boolean saveToFile;

  @CommandLine.Mixin
  ConfirmationPrompt confirmationPromptOption;

  @Override
  protected void execute() {
    confirmationPromptOption.confirmOrThrow(
        "Generating a new Terra Ssh key will replace the old Terra Ssh key if existed. "
            + "You must un-associate the old SSH public key from your GitHub account. "
            + "Are you sure you want to proceed (y/N)?",
        "Generating new SSH key proceeded"
    );
    var ecmService = ExternalCredentialsManagerService.fromContext();
    var sshKeyPair = ecmService.generateSshKeyPair(SshKeyPairType.GITHUB);
    if (saveToFile) {
      try {
        BufferedWriter privateWriter = new BufferedWriter(new FileWriter("terra_id_rsa"));
        privateWriter.write(sshKeyPair.getPrivateKey());
        privateWriter.close();
        BufferedWriter publicWriter = new BufferedWriter(new FileWriter("terra_id_rsa.pub"));
        publicWriter.write(sshKeyPair.getPublicKey());
        publicWriter.close();
        OUT.println(
            "Ssh private key is saved in terra_id_rsa and Ssh public key is saved in terra_id_rsa.pub. You can move them under ~/.ssh/.");
      } catch (IOException e) {
        OUT.println("Failed to write to file");
      }
    } else {
      formatOption.printReturnValue(UFSshKeyPair.createUFSshKey(sshKeyPair), UFSshKeyPair::print);
    }
  }
}
