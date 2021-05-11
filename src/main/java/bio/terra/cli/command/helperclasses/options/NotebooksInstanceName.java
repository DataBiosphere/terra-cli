package bio.terra.cli.command.helperclasses.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --instanceId and --location option for `terra notebooks`
 * commands.
 *
 * <p>A notebook is uniquely identified in GCP with a (projectId, location, instanceId). We get the
 * project id implicitly from the workspace context.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class NotebooksInstanceName {

  @CommandLine.Option(
      names = "--instance-id",
      required = true,
      description = "The id of the notebook instance.")
  public String instanceId;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance.")
  public String location;
}
