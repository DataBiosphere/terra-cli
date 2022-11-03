package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.PassthroughException;
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

  private static final Logger logger = LoggerFactory.getLogger(Git.class);

  @CommandLine.Option(
      names = "--gcs-bucket",
      description = "specify the gcs buket for gcloud builds")
  public String bucketName;

  @CommandLine.Option(names = "builds", description = "")
  public boolean builds;

  @CommandLine.Option(names = "submit", description = "")
  public boolean submit;

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

    if (bucketName != null && builds && submit) {
      var resource = Context.requireWorkspace().getResource(bucketName);
      if (Resource.Type.GCS_BUCKET != resource.getResourceType()) {
        throw new UserActionableException(
            String.format(
                "%s %s cannot be cloned because it is not a gcs-bucket",
                resource.getResourceType(), resource.getName()));
      }
      ArrayList<String> cloneCommands =
          new ArrayList<>(
              ImmutableList.of(
                  "gcloud",
                  "builds",
                  "submit",
                  "--async",
                  "--timeout=2h",
                  "--gcs-source-staging-dir=" + resource.resolve() + "/cloudbuild_source",
                  "--gcs-log-dir=" + resource.resolve() + "/cloudbuild_logs",
                  "--tag="
                      + "us-central1-docker.pkg.dev/$GOOGLE_CLOUD_PROJECT/ml4h/papermill:`date +'%Y%m%d'`"));
      cloneCommands.addAll(command);

      try {
        Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(cloneCommands);
      } catch (PassthroughException e) {
        ERR.println("gcloud builds submit for " + resource.getName() + " failed");
      }
      return;
    }
    // If user doesn't specify gcs-bucket, we still need to add back the digested `builds` and
    // `submit` commands
    if (submit) {
      command.add(0, "submit");
    }
    if (builds) {
      command.add(0, "builds");
    }
    command.add(0, getExecutableName());
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }
}
