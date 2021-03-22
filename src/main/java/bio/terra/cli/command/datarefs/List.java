package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs list" command. */
@CommandLine.Command(name = "list", description = "List all data references.")
public class List extends CommandWithFormatOptions<java.util.List<CloudResource>> {

  @Override
  protected java.util.List<CloudResource> execute() {
    return new WorkspaceManager(globalContext, workspaceContext).listDataReferences();
  }

  @Override
  protected void printText(java.util.List<CloudResource> returnValue) {
    for (CloudResource dataReference : returnValue) {
      System.out.println(
          dataReference.name + " (" + dataReference.type + "): " + dataReference.cloudId);
    }
  }
}
