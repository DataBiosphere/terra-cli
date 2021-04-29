package bio.terra.cli.command.helperclasses.options;

import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

public class CreateResource extends ResourceName {
  @CommandLine.Option(names = "--description", description = "Description of the resource")
  public String description;

  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}")
  public CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;
}
