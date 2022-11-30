package bio.terra.cli.command.user.sshkey;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSshKeyPair;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPairType;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra user ssh-key get" command. */
@Command(name = "get", description = "Get a terra-generated and -managed ssh key.")
public class Get extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  @Override
  protected void execute() {
    ExternalCredentialsManagerService ecmService = ExternalCredentialsManagerService.fromContext();
    var sshKeyPair = ecmService.getSshKeyPair(SshKeyPairType.GITHUB);
    formatOption.printReturnValue(UFSshKeyPair.createUFSshKey(sshKeyPair), UFSshKeyPair::print);
  }
}
