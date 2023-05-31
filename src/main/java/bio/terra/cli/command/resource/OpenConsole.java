package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.utils.UserIO;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resource open-console" command. */
@Command(name = "open-console", description = "Retrieve console link to access a cloud resource.")
public class OpenConsole extends WsmBaseCommand {
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

  /** Get an authenticated URL to directly access the resource in AWS console. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    UserIO.browse(resource.getConsoleUrl(scope, duration).toString());
  }
}
