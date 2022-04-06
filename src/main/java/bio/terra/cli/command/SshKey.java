package bio.terra.cli.command;

import bio.terra.cli.command.user.sshkey.Get;
import bio.terra.cli.command.user.sshkey.Regenerate;
import picocli.CommandLine.Command;

@Command(
    name = "sshkey",
    description = "Manage an terra managed ssh key pair.",
    subcommands = {Get.class, Regenerate.class})
public class SshKey {}
