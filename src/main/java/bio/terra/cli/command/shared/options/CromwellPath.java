package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

public class CromwellPath {
  @CommandLine.Option(
      names = "--path",
      required = false,
      description =
          "Cromwell path. For example, if not defined, the default path will be \"cromwell.conf\"; or \"home/jupyter/cromwell/cromwell.conf\"")
  public String path;
}
