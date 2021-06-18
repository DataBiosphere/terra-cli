package bio.terra.cli.command.config.set;

import bio.terra.cli.command.workspace.Set;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the fourth-level "terra config set workspace" command. It is exactly
 * the same command as "terra workspace set".
 */
@Command(name = "workspace", description = "Set the workspace to an existing one.")
public class Workspace extends Set {}
