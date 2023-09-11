package bio.terra.cli.command.workspace;

import static bio.terra.cli.utils.AwsConfiguration.AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE;
import static bio.terra.cli.utils.AwsConfiguration.DEFAULT_AWS_VAULT_PATH;
import static bio.terra.cli.utils.AwsConfiguration.DEFAULT_CACHE_WITH_AWS_VAULT;
import static bio.terra.cli.utils.AwsConfiguration.DEFAULT_TERRA_PATH;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.utils.AwsConfiguration;
import java.nio.file.Path;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra workspace configure-aws" command. */
@CommandLine.Command(
    name = "configure-aws",
    description = "Generate an AWS configuration file for a workspace.")
public class ConfigureAws extends WsmBaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Option(
      names = "--cache-with-aws-vault",
      description = "Use aws-vault for caching. Default - " + DEFAULT_CACHE_WITH_AWS_VAULT)
  private Boolean cacheWithAwsVault;

  @CommandLine.Option(
      names = "--terra-path",
      description = "Location of terra executable. Default - " + DEFAULT_TERRA_PATH)
  private String terraPath;

  @CommandLine.Option(
      names = "--aws-vault-path",
      description = "Location of aws-vault executable. Default - " + DEFAULT_AWS_VAULT_PATH)
  private String awsVaultPath;

  @CommandLine.Option(
      names = "--default-resource",
      description = "Name of resource to treat as default")
  private String defaultResourceName;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    AwsConfiguration.Builder builder =
        AwsConfiguration.builder().setWorkspace(Context.requireWorkspace());
    if (cacheWithAwsVault != null) {
      builder.setCacheWithAwsVault(cacheWithAwsVault);
    }
    if (terraPath != null) {
      builder.setTerraPath(terraPath);
    }
    if (awsVaultPath != null) {
      builder.setAwsVaultPath(awsVaultPath);
    }
    if (defaultResourceName != null) {
      builder.setDefaultResourceName(defaultResourceName);
    }

    Path filePath = builder.build().storeToDisk();
    OUT.printf("export %s=%s%n", AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE, filePath);
  }
}
