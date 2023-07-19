package bio.terra.cli.command;

import bio.terra.cli.command.folder.SetProperty;
import bio.terra.cli.command.folder.Tree;
import picocli.CommandLine.Command;

@Command(
    name = "folder",
    description = "Commands related to folder.",
    subcommands = {Tree.class, SetProperty.class})
public class Folder {}
