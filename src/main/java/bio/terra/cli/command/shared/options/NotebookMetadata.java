package bio.terra.cli.command.shared.options;

import bio.terra.workspace.model.GcpAiNotebookUpdateParameters;
import java.util.Map;
import picocli.CommandLine;

/**
 * Command helper class that defines the --new-metadata option for `terra resource update
 * gcp-notebook` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class NotebookMetadata {
  @CommandLine.Option(names = "--new-metadata", description = "Metadata of the Gcp Notebook.")
  public Map<String, String> newMetadata;

  public GcpAiNotebookUpdateParameters getMetadata() {
    return new GcpAiNotebookUpdateParameters().metadata(newMetadata);
  }

  public boolean isDefined() {
    return newMetadata != null;
  }
}
