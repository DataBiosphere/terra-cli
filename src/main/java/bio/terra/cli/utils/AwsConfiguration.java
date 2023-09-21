package bio.terra.cli.utils;

import static bio.terra.cli.utils.AwsConfiguration.Builder.OPT_AWS_VAULT_PATH;
import static bio.terra.cli.utils.AwsConfiguration.Builder.OPT_CACHE_WITH_AWS_VAULT;
import static bio.terra.cli.utils.AwsConfiguration.Builder.OPT_DEFAULT_RESOURCE_NAME;
import static bio.terra.cli.utils.AwsConfiguration.Builder.OPT_TERRA_PATH;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Resource.CredentialsAccessScope;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.exception.SystemException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

/**
 * Class to represent an AWS CLI/SDK profile for all the AWS resources in a workspace, and write it
 * in <a href="https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html">the
 * expected format</a>.
 *
 * <p>This makes use of class {@link Ini}, which writes files in the ini format used by AWS config
 * files.
 */
public class AwsConfiguration {
  public static final String AWS_CONTEXT_SUBDIRECTORY_NAME = "aws";
  public static final String AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE = "AWS_CONFIG_FILE";
  public static final boolean DEFAULT_CACHE_WITH_AWS_VAULT = false;
  public static final String DEFAULT_TERRA_PATH = "/usr/local/bin/terra";
  public static final String DEFAULT_AWS_VAULT_PATH = "/usr/local/bin/aws-vault";
  private static final String defaultProfileSectionName = "default";
  private static final Set<Resource.Type> supportedResourceTypes =
      Set.of(Resource.Type.AWS_S3_STORAGE_FOLDER, Resource.Type.AWS_SAGEMAKER_NOTEBOOK);
  private final Path filePath;
  private final Map<String, String> optionsMap;
  private final Ini ini;

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .setCacheWithAwsVault(getCacheWithAwsVault())
        .setTerraPath(getTerraPath())
        .setAwsVaultPath(getAwsVaultPath())
        .setDefaultResourceName(getDefaultResourceName().orElse(null));
  }

  private AwsConfiguration(Builder builder) {
    Workspace workspace = builder.workspace;
    filePath = getConfigFilePath(workspace.getUuid());
    ini = new Ini();

    optionsMap = builderToOptionsMap(builder);
    ini.setComment(optionsMapToString());

    workspace.listResources().stream()
        .filter(resource -> supportedResourceTypes.contains(resource.getResourceType()))
        .forEach(
            resource ->
                addAwsResourceProfiles(
                    resource.getName(),
                    resource.getRegion(),
                    builder.cacheWithAwsVault,
                    builder.terraPath,
                    builder.awsVaultPath,
                    resource.getName().equals(builder.defaultResourceName)));
  }

  private AwsConfiguration(Path filePath) throws IOException {
    this.filePath = filePath;
    this.ini = new Ini(filePath.toFile());
    this.optionsMap = stringToOptionsMap(ini.getComment());
  }

  @Override
  public String toString() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      ini.store(stream);
    } catch (IOException e) {
      throw new SystemException("Writing configuration to file failed.", e);
    }
    return stream.toString();
  }

  private Map<String, String> builderToOptionsMap(Builder builder) {
    Map<String, String> map = new HashMap<>();
    map.put(Builder.OPT_CACHE_WITH_AWS_VAULT, String.valueOf(builder.cacheWithAwsVault));
    map.put(Builder.OPT_TERRA_PATH, builder.terraPath);
    map.put(Builder.OPT_AWS_VAULT_PATH, builder.awsVaultPath);

    if (builder.defaultResourceName != null) {
      map.put(Builder.OPT_DEFAULT_RESOURCE_NAME, builder.defaultResourceName);
    }
    return map;
  }

  private String optionsMapToString() {
    return optionsMap.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(", "));
  }

  private static Map<String, String> stringToOptionsMap(String options) {
    return Arrays.stream(options.split(", "))
        .map(entry -> entry.split("="))
        .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
  }

  private void addAwsResourceProfiles(
      // Resource resource,
      String resourceName,
      String region,
      boolean cacheWithAwsVault,
      String terraPath,
      String awsVaultPath,
      boolean isDefault) {
    String profileName = getProfileName(resourceName);
    String readOnlyProfileName = getReadOnlyProfileName(profileName);

    Optional<Profile> defaultProfile = Optional.empty();

    if (cacheWithAwsVault) {
      // Add Caching Profiles: Caching profile names should track resource names.
      // We append an underscore to "real" profile names, which now serve as targets for the
      // caching profiles. Note that this side effect is intentionally applied to all subsequent
      // statements, including those outside this block.
      String cachingProfileName = profileName;
      String cachingReadOnlyProfileName = readOnlyProfileName;
      profileName += "_";
      readOnlyProfileName += "_";

      Section section = ini.add(getNamedProfileSectionName(cachingProfileName));
      Profile profile = buildCachingProfile(region, awsVaultPath, profileName);
      addProfileToSection(section, profile);
      if (isDefault) {
        defaultProfile = Optional.of(profile);
        isDefault = false;
      }

      section = ini.add(getNamedProfileSectionName(cachingReadOnlyProfileName));
      profile = buildCachingProfile(region, awsVaultPath, readOnlyProfileName);
      addProfileToSection(section, profile);
    }

    Section section = ini.add(getNamedProfileSectionName(profileName));
    Profile profile =
        buildResourceProfile(
            region, terraPath, resourceName, CredentialsAccessScope.WRITE_READ.toString());
    addProfileToSection(section, profile);
    if (isDefault) {
      defaultProfile = Optional.of(profile);
    }

    section = ini.add(getNamedProfileSectionName(readOnlyProfileName));
    profile =
        buildResourceProfile(
            region, terraPath, resourceName, CredentialsAccessScope.READ_ONLY.toString());
    addProfileToSection(section, profile);

    defaultProfile.ifPresent(p -> addProfileToSection(ini.add(defaultProfileSectionName), p));
  }

  public void addResource(String resourceName, String region, Resource.Type resourceType) {
    if (supportedResourceTypes.contains(resourceType)) {
      addAwsResourceProfiles(
          resourceName,
          region,
          Boolean.parseBoolean(optionsMap.get(Builder.OPT_CACHE_WITH_AWS_VAULT)),
          optionsMap.get(Builder.OPT_TERRA_PATH),
          optionsMap.get(Builder.OPT_AWS_VAULT_PATH),
          false);
    }
  }

  public void removeResource(String resourceName) {
    String profileName = getProfileName(resourceName);
    String readOnlyProfileName = getReadOnlyProfileName(profileName);

    Optional.ofNullable(ini.get(getNamedProfileSectionName(profileName))).ifPresent(ini::remove);
    Optional.ofNullable(ini.get(getNamedProfileSectionName(readOnlyProfileName)))
        .ifPresent(ini::remove);

    Optional.ofNullable(ini.get(getNamedProfileSectionName(profileName + "_")))
        .ifPresent(ini::remove);
    Optional.ofNullable(ini.get(getNamedProfileSectionName(readOnlyProfileName + "_")))
        .ifPresent(ini::remove);

    if (resourceName.equals(optionsMap.getOrDefault(Builder.OPT_DEFAULT_RESOURCE_NAME, null))) {
      Optional.ofNullable(ini.get(defaultProfileSectionName)).ifPresent(ini::remove);
      optionsMap.remove(Builder.OPT_DEFAULT_RESOURCE_NAME);
      ini.setComment(optionsMapToString());
    }
  }

  public Path getFilePath() {
    return filePath;
  }

  public String getTerraPath() {
    return optionsMap.get(OPT_TERRA_PATH);
  }

  public String getAwsVaultPath() {
    return optionsMap.get(OPT_AWS_VAULT_PATH);
  }

  public boolean getCacheWithAwsVault() {
    return Boolean.parseBoolean(optionsMap.get(OPT_CACHE_WITH_AWS_VAULT));
  }

  public Optional<String> getDefaultResourceName() {
    return optionsMap.containsKey(OPT_DEFAULT_RESOURCE_NAME)
        ? Optional.of(optionsMap.get(OPT_DEFAULT_RESOURCE_NAME))
        : Optional.empty();
  }

  public int getResourceCount() {
    return ini.size();
  }

  // Disk operations

  public static Path getConfigFilePath(UUID workspaceUuid) {
    return Context.getContextDir()
        .resolve(AWS_CONTEXT_SUBDIRECTORY_NAME)
        .resolve(String.format("%s.conf", workspaceUuid.toString()));
  }

  public Path storeToDisk() {
    try {
      FileUtils.writeStringToFile(filePath.toFile(), this.toString());
      return filePath;
    } catch (IOException e) {
      throw new SystemException("Error writing configuration file to disk.", e);
    }
  }

  public static void deleteFromDisk(UUID workspaceUuid) {
    try {
      FileUtils.delete(getConfigFilePath(workspaceUuid));
    } catch (IOException e) {
      throw new SystemException("Error deleting configuration file from disk.", e);
    }
  }

  public static AwsConfiguration loadFromDisk(UUID workspaceUuid) {
    try {
      Path filePath = AwsConfiguration.getConfigFilePath(workspaceUuid);
      return new AwsConfiguration(filePath);
    } catch (IOException e) {
      throw new SystemException("Error reading configuration file from disk.", e);
    }
  }

  // Profile

  /**
   * Record type for specifying the 'region' and 'credential_process' configuration parameters for a
   * given AWS profile representing a single AWS resource.
   *
   * @param region region in which the resource exists
   * @param credentialProcess executable to execute in order to obtain a temporary credential to
   *     access the resource.
   */
  public record Profile(String region, List<String> credentialProcess) {}

  /**
   * Factory method for creating {@link Profile} instances.
   *
   * @param region region of an AWS resource
   * @param credentialProcess command to call to obtain a credential for the resource
   * @return a {@link Profile} instance
   */
  public static Profile createProfile(String region, List<String> credentialProcess) {
    return new Profile(region, credentialProcess);
  }

  private static void addProfileToSection(Section section, Profile profile) {
    section.add("region", profile.region());
    section.add("credential_process", String.join(" ", profile.credentialProcess));
  }

  public static String getProfileName(String resourceName) {
    return String.format("%s", resourceName);
  }

  public static String getReadOnlyProfileName(String profileName) {
    return String.format("%s-ro", profileName);
  }

  private static String getNamedProfileSectionName(String name) {
    return String.format("profile %s", name);
  }

  public static List<String> buildCachingCommandLine(
      String awsVaultPath, String targetProfileName) {
    return List.of(awsVaultPath, "export", "--no-session", "--format", "json", targetProfileName);
  }

  private static Profile buildCachingProfile(
      String region, String awsVaultPath, String targetProfileName) {
    return AwsConfiguration.createProfile(
        region, buildCachingCommandLine(awsVaultPath, targetProfileName));
  }

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

  private static Profile buildResourceProfile(
      String region, String terraPath, String resourceName, String access) {
    return AwsConfiguration.createProfile(
        region, buildResourceCommandLine(terraPath, resourceName, access));
  }

  public static class Builder {
    public static final String OPT_CACHE_WITH_AWS_VAULT = "cacheWithAwsVault";
    public static final String OPT_TERRA_PATH = "terraPath";
    public static final String OPT_AWS_VAULT_PATH = "awsVaultPath";
    public static final String OPT_DEFAULT_RESOURCE_NAME = "defaultResourceName";

    private boolean cacheWithAwsVault;
    private String terraPath;
    private String awsVaultPath;
    private String defaultResourceName;
    private Workspace workspace;

    private Builder() {
      cacheWithAwsVault = DEFAULT_CACHE_WITH_AWS_VAULT;
      terraPath = DEFAULT_TERRA_PATH;
      awsVaultPath = DEFAULT_AWS_VAULT_PATH;
      defaultResourceName = null; // no default
      workspace = null; // no default
    }

    public Builder setCacheWithAwsVault(boolean cacheWithAwsVault) {
      this.cacheWithAwsVault = cacheWithAwsVault;
      return this;
    }

    public Builder setTerraPath(String terraPath) {
      this.terraPath = terraPath;
      return this;
    }

    public Builder setAwsVaultPath(String awsVaultPath) {
      this.awsVaultPath = awsVaultPath;
      return this;
    }

    public Builder setDefaultResourceName(String defaultResourceName) {
      this.defaultResourceName = defaultResourceName;
      return this;
    }

    public Builder setWorkspace(Workspace workspace) {
      this.workspace = workspace;
      return this;
    }

    /** Build the configuration */
    public AwsConfiguration build() {
      if (workspace == null) {
        throw new SystemException("Workspace not set for AWS configuration");
      }
      return new AwsConfiguration(this);
    }
  }
}
