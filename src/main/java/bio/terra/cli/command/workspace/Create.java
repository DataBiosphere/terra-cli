package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceNameAndDescription;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.workspace.model.CloudPlatform;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace create" command. */
@Command(
    name = "create",
    description = "Create a new workspace.")
  //  modelTransformer = Create.SubCmdFilter.class)
public class Create extends BaseCommand {
 /* private static final Logger logger = LoggerFactory.getLogger(Create.class);

  static class SubCmdFilter implements CommandLine.IModelTransformer {
    public CommandLine.Model.CommandSpec transform(CommandLine.Model.CommandSpec commandSpec) {
      logger.debug("TEST - Dex 1");

      try {
        logger.debug("TEST - Dex 2");
        List<CloudPlatform> supportedCloudPlatforms =
            Context.getServer().getSupportedCloudPlatforms();
        OptionSpec curSpec = commandSpec.findOption("--platform");

        if (supportedCloudPlatforms == null || supportedCloudPlatforms.size() < 2) {
          logger.debug("TEST - Dex 3");
          OptionSpec newSpec = OptionSpec.builder(curSpec).hidden(true).build();
          commandSpec.remove(curSpec);
          commandSpec.addOption(newSpec);
          /*  commandSpec.addOption(
               OptionSpec.builder("--platform")
                   .usageHelp(true)
                   .completionCandidates(supportedCloudPlatforms.stream().map(Objects::toString).collect(Collectors.toList()))
                   .description("Set the Cloud platform: ${COMPLETION-CANDIDATES}.")
                   .build());


        }
        logger.debug("TEST - Dex 4");
      } catch (SystemException e) {
        logger.debug("TEST - Dex 5 - " + e.getMessage());
        return commandSpec;
      }
      logger.debug("TEST - Dex 6");
      return commandSpec;
    }
  }

  static class CloudPlatformCandidates implements Iterable<String> {
    static java.util.List<String> supportedCloudPlatforms = new ArrayList<>();

    @Override
    public Iterator<String> iterator() {
      return supportedCloudPlatforms.iterator();
    }
  }
*/
  @CommandLine.Mixin WorkspaceNameAndDescription workspaceNameAndDescription;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--properties",
      required = false,
      split = ",",
      description =
          "Workspace properties. Example: --properties=key=value. For multiple properties, use \",\": --properties=key1=value1,key2=value2")
  public Map<String, String> workspaceProperties;

  @CommandLine.Option(names = "--id", required = true, description = "Workspace ID")
  // Variable is `id` instead of `userFacingId` because user sees it with `terra workspace create`
  private String id;

  @CommandLine.Option(
      names = "--platform",
      description = "Set the Cloud platform: ${COMPLETION-CANDIDATES}.")
  private CloudPlatform cloudPlatform;

  /** Create a new workspace. */
  @Override
  protected void execute() {
    Workspace workspace =
        Workspace.create(
            id,
            CloudPlatform.GCP, // Currently only GCP is supported
            workspaceNameAndDescription.name,
            workspaceNameAndDescription.description,
            workspaceProperties);
    formatOption.printReturnValue(new UFWorkspace(workspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace successfully created.");
    returnValue.print();
  }
}
