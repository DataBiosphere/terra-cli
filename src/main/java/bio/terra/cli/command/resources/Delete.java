package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources delete" command. */
@CommandLine.Command(name = "delete", description = "Delete a resource from the workspace.")
public class Delete extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin Format formatOption;

  /** Delete a resource from the workspace. */
  @Override
  protected void execute() {
    Resource resource =
        GlobalContext.get().requireCurrentWorkspace().getResource(resourceNameOption.name);
    resource.delete();
    formatOption.printReturnValue(resource, Delete::printText);

    //    } else if (resourceToDelete
    //        .getMetadata()
    //        .getStewardshipType()
    //        .equals(StewardshipType.CONTROLLED)) {
    //      switch (resourceToDelete.getMetadata().getResourceType()) {
    //        case AI_NOTEBOOK:
    //          workspaceManager.deleteControlledAiNotebookInstance(resourceNameOption.name);
  }

  /** Print this command's output in text format. */
  private static void printText(Resource returnValue) {
    OUT.println("Successfully deleted resource.");
    returnValue.printText();
  }
}
