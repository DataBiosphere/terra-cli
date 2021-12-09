package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource delete" command. */
@CommandLine.Command(name = "delete", description = "Delete a resource from the workspace.")
public class Delete extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Delete.class);
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin ResourceName resourceNameOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Delete a resource from the workspace. */
  @Override
  protected void execute() {
    logger.debug("terra resource delete --name=" + resourceNameOption.name);
    workspaceOption.overrideIfSpecified();
    Resource resourceToDelete = Context.requireWorkspace().getResource(resourceNameOption.name);

    // print details about the resource before showing the delete prompt
    resourceToDelete.serializeToCommand().print();
    deletePromptOption.confirmOrThrow();

    resourceToDelete.delete();
    OUT.println("Resource successfully deleted.");
  }
}
