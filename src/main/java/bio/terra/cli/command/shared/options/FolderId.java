package bio.terra.cli.command.shared.options;

import java.util.UUID;
import picocli.CommandLine;

public class FolderId {

  @CommandLine.Option(names = "--id", required = true, description = "folder id")
  public UUID folderId;
}
