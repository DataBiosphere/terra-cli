package bio.terra.cli.command;

import bio.terra.cli.command.user.sshkey.Generate;
import bio.terra.cli.command.user.sshkey.Get;
import picocli.CommandLine.Command;

@Command(
    name = "ssh-key",
    description = "Manage an terra managed ssh key pair.",
    subcommands = {Get.class, Generate.class})
public class SshKey {}
