package bio.terra.cli.command;

import bio.terra.cli.command.folder.Create;
import bio.terra.cli.command.folder.Delete;
import bio.terra.cli.command.folder.SetProperty;
import bio.terra.cli.command.folder.Tree;
import bio.terra.cli.command.folder.Update;
import picocli.CommandLine.Command;

@Command(
    name = "folder",
    description = "Commands related to folder.",
    subcommands = {Tree.class, SetProperty.class, Create.class, Delete.class, Update.class})
public class Folder {}
