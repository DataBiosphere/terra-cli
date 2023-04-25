package bio.terra.cli.command.user.sshkey;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPair;
import bio.terra.externalcreds.model.SshKeyPairType;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra user ssh-key get" command. */
@Command(name = "get", description = "Get a Terra-generated and -managed SSH key.")
public class Get extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(names = "--include-private-key", description = "Include private ssh key.")
  private boolean includePrivateKey;

  @Override
  protected void execute() {
    ExternalCredentialsManagerService ecmService = ExternalCredentialsManagerService.fromContext();
    // Post-startup.sh is parsing the private key for the json and store it in the notebook.
    // https://github.com/DataBiosphere/terra-workspace-manager/blob/c8c17e456142577d49fc8879c43ef9f034feb884/service/src/main/java/bio/terra/workspace/service/resource/controlled/cloud/gcp/ainotebook/post-startup.sh#L185
    SshKeyPair sshKeyPair =
        ecmService.getSshKeyPair(SshKeyPairType.GITHUB, /*includePrivateKey=*/ includePrivateKey);
    formatOption.printReturnValue(UFSshKeyPair.createUFSshKey(sshKeyPair), UFSshKeyPair::print);
  }
}
