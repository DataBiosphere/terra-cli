package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.exceptions.FlagsmithApiError;
import com.flagsmith.models.Flags;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(
    name = "gcloud",
    description = "Call gcloud in the Terra workspace.",
    modelTransformer = Gcloud.SubCmdFilter.class)
public class Gcloud extends ToolCommand {
  static class SubCmdFilter implements CommandLine.IModelTransformer {
    public CommandLine.Model.CommandSpec transform(CommandLine.Model.CommandSpec commandSpec) {
      try {
        FlagsmithClient flagsmith =
            FlagsmithClient.newBuilder().setApiKey("38bPsuqpMxsKU33QshiHSx").build();

        Flags flags = flagsmith.getEnvironmentFlags();

        if (!Context.getServer().getCloudBuildEnabled()) {
          // && !flags.isFeatureEnabled("terra__cloud_build_enabled")) {
          commandSpec.removeSubcommand("--gcs-bucket-resource");
        }
      } catch (SystemException e) {
        return commandSpec;
      } catch (FlagsmithApiError e) {
        commandSpec.removeSubcommand("--gcs-bucket-resource");
        return commandSpec;
      }
      return commandSpec;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(Gcloud.class);

  @CommandLine.Option(
      names = "--gcs-bucket-resource",
      description =
          "Optional flag only used for `terra gcloud builds submit`. Resource name (not bucket name) of GCS bucket where Cloud Build source and logs will be stored.")
  public String gcsBucketResourceName;

  @Override
  public String getExecutableName() {
    return "gcloud";
  }

  @Override
  public String getInstallationUrl() {
    return "https://cloud.google.com/sdk/docs/install";
  }

  @Override
  protected void executeImpl() {
    workspaceOption.overrideIfSpecified();
    command.add(0, getExecutableName());

    if (gcsBucketResourceName != null) {
      var resource = Context.requireWorkspace().getResource(gcsBucketResourceName);
      if (Resource.Type.GCS_BUCKET != resource.getResourceType()) {
        throw new UserActionableException(
            String.format(
                "--Resource %s was passed into --gcs-bucket-resource, but it is not a GCS bucket. It is %s",
                resource.getResourceType(), resource.getName()));
      }
      ArrayList<String> autoCommands =
          new ArrayList<>(
              ImmutableList.of(
                  "--gcs-source-staging-dir=" + resource.resolve() + "/cloudbuild_source",
                  "--gcs-log-dir=" + resource.resolve() + "/cloudbuild_logs"));

      logger.info("Final gcloud command: " + command + autoCommands);
      command.addAll(autoCommands);
    }

    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }
}
