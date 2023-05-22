package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.AwsConfiguration;
import bio.terra.cli.utils.FileUtils;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra workspace configure-aws" command. */
@CommandLine.Command(
    name = "configure-aws",
    description = "Generate an AWS configuration file for a workspace.")
public class ConfigureAws extends WsmBaseCommand {
  private static final String AWS_CONTEXT_SUBDIRECTORY_NAME = "aws";
  public static final String DEFAULT_TERRA_PATH = "/usr/local/bin/terra";
  public static final String DEFAULT_AWS_VAULT_PATH = "/usr/local/bin/aws-vault";
  public static final String AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE = "AWS_CONFIG_FILE";

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Option(
      names = "--cache-with-aws-vault",
      defaultValue = "false",
      description = "Use aws-vault for caching")
  private boolean cacheWithAwsVault;

  @CommandLine.Option(
      names = "--terra-path",
      defaultValue = DEFAULT_TERRA_PATH,
      description = "Location of terra executable.")
  private String terraPath;

  @CommandLine.Option(
      names = "--aws-vault-path",
      defaultValue = DEFAULT_AWS_VAULT_PATH,
      description = "Location of aws-vault executable.")
  private String awsVaultPath;

  @CommandLine.Option(
      names = "--default-resource",
      required = false,
      description = "Name of resource to treat as default")
  private String defaultResourceName;

  private static Path getAwsContextDir() {
    return Context.getContextDir()
        .resolve(AWS_CONTEXT_SUBDIRECTORY_NAME);
  }

  private static Path getConfigFilePath(Workspace workspace) {
    return getAwsContextDir().resolve(String.format("%s.conf", workspace.getUuid()));
  }

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    Workspace workspace = Context.requireWorkspace();

    AwsConfiguration.Builder builder = AwsConfiguration.builder();
    List<Resource> resourceList = Context.requireWorkspace().listResources();

    for (Resource resource : resourceList) {
      switch (resource.getResourceType()) {
        case AWS_S3_STORAGE_FOLDER, AWS_SAGEMAKER_NOTEBOOK -> generateAwsResourceProfiles(builder, resource);
      }
    }

    AwsConfiguration awsConfiguration = builder.build();

    if (awsConfiguration.size() == 0) {
      throw new UserActionableException(
          "Workspace contains no AWS resources to configure profiles for.");
    }

    Path configFilePath = getConfigFilePath(workspace);

    try {
      FileUtils.writeStringToFile(configFilePath.toFile(), awsConfiguration.toString());
    } catch (IOException e) {
      throw new SystemException("Error writing configuration file to disk.", e);
    }

    OUT.printf("export %s=%s%n", AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE, configFilePath);
  }

  private void generateAwsResourceProfiles(AwsConfiguration.Builder builder, Resource resource) {

    String resourceName = resource.getName();
    boolean isDefault = resourceName.equals(defaultResourceName);

    String region = resource.getRegion();
    String profileName = String.format("%s", resourceName);
    String readOnlyProfileName = String.format("%s-ro", profileName);

    if (cacheWithAwsVault) {
      // Add Caching Profiles

      // Caching profile names should track resource names.  We append an underscore to "real"
      // profile names, which now serve as targets for the caching profiles.  Note that this side
      // effect is intentionally applied to all subsequent statements, including those outside this
      // block.
      String cachingProfileName = profileName;
      String cachingReadOnlyProfileName = readOnlyProfileName;
      profileName += "_";
      readOnlyProfileName += "_";

      addCachingProfile(builder, region, cachingProfileName, isDefault, profileName);
      addCachingProfile(builder, region, cachingReadOnlyProfileName, false, readOnlyProfileName);

      // clear isDefault flag
      isDefault = false;
    }

    addAwsResourceProfile(builder, region, profileName, isDefault, resourceName, "WRITE_READ");
    addAwsResourceProfile(builder, region, readOnlyProfileName, false, resourceName, "READ_ONLY");
  }

  @VisibleForTesting
  public static List<String> buildResourceCommandLine(
      String terraPath, String resourceName, String access) {
    return List.of(
        terraPath,
        "resource",
        "credentials",
        "--name",
        resourceName,
        "--scope",
        access,
        "--format",
        "JSON");
  }

  private void addAwsResourceProfile(
      AwsConfiguration.Builder builder,
      String region,
      String profileName,
      boolean isDefault,
      String resourceName,
      String access) {
    builder.addProfile(
        profileName,
        AwsConfiguration.createProfile(
            region, buildResourceCommandLine(terraPath, resourceName, access)),
        isDefault);
  }

  @VisibleForTesting
  public static List<String> buildCachingCommandLine(
      String awsVaultPath, String targetProfileName) {
    return List.of(awsVaultPath, "export", "--no-session", "--format", "json", targetProfileName);
  }

  private void addCachingProfile(
      AwsConfiguration.Builder builder,
      String region,
      String profileName,
      boolean isDefault,
      String targetProfileName) {
    builder.addProfile(
        profileName,
        AwsConfiguration.createProfile(
            region, buildCachingCommandLine(awsVaultPath, targetProfileName)),
        isDefault);
  }
}
