package bio.terra.cli.command;

import bio.terra.cli.command.user.sshkey.Add;
import bio.terra.cli.command.user.sshkey.Generate;
import bio.terra.cli.command.user.sshkey.Get;
import picocli.CommandLine.Command;

@Command(
    name = "ssh-key",
    description = "Get and generate an terra managed ssh key pair.",
    subcommands = {Get.class, Generate.class, Add.class})
public class SshKey {}
