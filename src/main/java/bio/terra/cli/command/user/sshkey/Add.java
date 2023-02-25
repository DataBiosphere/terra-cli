package bio.terra.cli.command.user.sshkey;

import static bio.terra.cli.utils.FileUtils.writeStringToFile;
import static org.apache.commons.io.FilenameUtils.concat;

import bio.terra.cli.app.LocalProcessCommandRunner;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPair;
import bio.terra.externalcreds.model.SshKeyPairType;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "add", description = "Save your Terra SSH key to ~/.ssh and add it to ssh-agent.")
public class Add extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  @Override
  protected void execute() {
    ExternalCredentialsManagerService ecmService = ExternalCredentialsManagerService.fromContext();
    SshKeyPair sshKeyPair =
        ecmService.getSshKeyPair(SshKeyPairType.GITHUB, /*includePrivateKey=*/ true);
    saveKeyFileAndSshAdd(sshKeyPair);
    formatOption.printReturnValue(UFSshKeyPair.createUFSshKey(sshKeyPair), UFSshKeyPair::print);
  }

  protected static void saveKeyFileAndSshAdd(SshKeyPair sshKeyPair) {
    String dir = System.getProperty("user.home") + "/.ssh/";
    String privateKeyPath = concat(dir, "terra_id_rsa");
    String publicKeyPath = concat(dir, "terra_id_rsa.pub");
    try {
      writeStringToFile(new File(privateKeyPath), sshKeyPair.getPrivateKey());
      writeStringToFile(new File(publicKeyPath), sshKeyPair.getPublicKey());
      OUT.println("Saved the terra ssh key to ~/.ssh/terra_id_rsa and ~/.ssh/terra_id_rsa.pub");
    } catch (IOException e) {
      OUT.print("Failed to write the ssh key to ${HOME}/.ssh");
    }

    // Removes read, write, execute permissions from the group and other users.
    // Then add the private key to the ssh agent.
    // chmod go-rwx ~/.ssh/terra_id_rsa && eval "$(ssh-agent -s)" && ssh-add ~/.ssh/terra_id_rsa
    LocalProcessCommandRunner.runBashCommand(
        LocalProcessCommandRunner.buildFullCommand(
            List.of(
                "chmod",
                "go-rwx",
                privateKeyPath,
                "&&",
                "eval \"$(ssh-agent -s)\"",
                "&&",
                "ssh-add",
                privateKeyPath)),
        Collections.emptyMap());
  }
}
