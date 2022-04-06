package bio.terra.cli.command.user.sshkey;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSshKey;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPairType;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get", description = "Get a terra generated and managed ssh key.")
public class Get extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  @Override
  protected void execute() {
    var ecmService = ExternalCredentialsManagerService.returnFromContext();
    var sshKeyPair = ecmService.getSshKeyPair(SshKeyPairType.GITHUB);
    formatOption.printReturnValue(
        UFSshKey.createUFSshKey(
            sshKeyPair.getPrivateKey(),
            sshKeyPair.getPublicKey(),
            sshKeyPair.getExternalUserEmail()),
        UFSshKey::print);
  }
}
