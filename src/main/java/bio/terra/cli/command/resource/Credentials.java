package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resource credentials" command. */
@Command(
    name = "credentials",
    description = "Retrieve temporary credentials to access a cloud resource.")
public class Credentials extends WsmBaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Option(
      names = "--scope",
      description = "Set the credentials access scope: ${COMPLETION-CANDIDATES}.",
      defaultValue = "READ_ONLY",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  private Resource.CredentialsAccessScope scope;

  @CommandLine.Option(
      names = "--duration",
      defaultValue = "900",
      description = "Duration of access (in seconds), minimum: 900, maximum: 3600")
  private int duration;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Resolve a resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    JSONObject object = resource.getCredentials(scope, duration);

    // TODO(TERRA-363): check if format.printText is sufficient
    formatOption.printReturnValue(object);
  }
}
