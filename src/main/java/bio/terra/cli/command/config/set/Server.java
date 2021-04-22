package bio.terra.cli.command.config.set;

import bio.terra.cli.command.server.Set;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the fourth-level "terra config set server" command. It is exactly the
 * same command as "terra server set".
 */
@Command(name = "server", description = "Set the Terra server to connect to.")
public class Server extends Set {}
