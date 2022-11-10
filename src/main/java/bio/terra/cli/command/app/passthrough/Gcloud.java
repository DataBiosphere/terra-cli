package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(name = "gcloud", description = "Call gcloud in the Terra workspace.")
public class Gcloud extends ToolCommand {

  private static final Logger logger = LoggerFactory.getLogger(Gcloud.class);

  @CommandLine.Option(
      names = "--gcs-bucket",
      description =
          "Resource name (not bucket name) of GCS bucket resource. Required for (and only used for) `gcloud builds submit`.")
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
                "%s %s cannot builds submit because it is not a gcs-bucket",
                resource.getResourceType(), resource.getName()));
      }
      ArrayList<String> autoCommands =
          new ArrayList<>(
              ImmutableList.of(
                  "--gcs-source-staging-dir=" + resource.resolve() + "/cloudbuild_source",
                  "--gcs-log-dir=" + resource.resolve() + "/cloudbuild_logs"));

      logger.info("run command: " + command + autoCommands);
      command.addAll(autoCommands);
    }
    // If user doesn't specify gcs-bucket, we still need to add back the digested `builds` and
    // `submit` commands
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }
}
