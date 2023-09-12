package harness.utils;

import static bio.terra.cli.utils.AwsConfiguration.AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.utils.AwsConfiguration;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;

public class AwsConfigurationTestUtils {
  private static final Pattern configOutputPattern = Pattern.compile("export (.*)=(.*)");

  public static Path getProfilePathFromOutput(String configOutput) {
    Matcher matcher = configOutputPattern.matcher(configOutput);
    assertTrue(matcher.find());
    assertEquals(2, matcher.groupCount());
    assertEquals(AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE, matcher.group(1));
    return Path.of(matcher.group(2));
  }

  private record ResourceProfile(String region, String credentialProcess) {}

  private static ResourceProfile toResourceProfile(Optional<Profile> profile) {
    assertTrue(profile.isPresent());

    Optional<String> property = profile.get().property("region");
    assertTrue(property.isPresent());

    Optional<String> credentialProcess = profile.get().property("credential_process");
    assertTrue(credentialProcess.isPresent());

    return new ResourceProfile(property.get(), credentialProcess.get());
  }

  private static void validateResourceProfile(
      String region,
      ResourceProfile resourceProfile,
      String terraPath,
      String resourceName,
      String access) {
    String expectedCommandLine =
        String.join(
            " ", AwsConfiguration.buildResourceCommandLine(terraPath, resourceName, access));
    assertEquals(expectedCommandLine, resourceProfile.credentialProcess());
    assertEquals(region, resourceProfile.region());
  }

  private static void validateCachedProfile(
      String region,
      ResourceProfile resourceProfile,
      String awsVaultPath,
      String targetProfileName) {
    String expectedCommandLine =
        String.join(" ", AwsConfiguration.buildCachingCommandLine(awsVaultPath, targetProfileName));
    assertEquals(expectedCommandLine, resourceProfile.credentialProcess());
    assertEquals(region, resourceProfile.region());
  }

  public static void validateConfiguration(
      AwsConfiguration awsConfiguration, String region, Collection<String> resourceNames) {
    Path configFilePath = awsConfiguration.getFilePath();
    String terraPath = awsConfiguration.getTerraPath();
    String awsVaultPath = awsConfiguration.getAwsVaultPath();
    boolean cacheWithAwsVault = awsConfiguration.getCacheWithAwsVault();
    Optional<String> defaultResourceName = awsConfiguration.getDefaultResourceName();

    ProfileFile profileFile =
        ProfileFile.builder().type(ProfileFile.Type.CONFIGURATION).content(configFilePath).build();

    for (String resourceName : resourceNames) {
      String readOnlyResourceName = String.format("%s-ro", resourceName);

      if (cacheWithAwsVault) {
        String suffixedResourceName = String.format("%s_", resourceName);
        validateResourceProfile(
            region,
            toResourceProfile(profileFile.profile(suffixedResourceName)),
            terraPath,
            resourceName,
            "WRITE_READ");
        validateCachedProfile(
            region,
            toResourceProfile(profileFile.profile(resourceName)),
            awsVaultPath,
            suffixedResourceName);

        String suffixedReadOnlyResourceName = String.format("%s_", readOnlyResourceName);
        validateResourceProfile(
            region,
            toResourceProfile(profileFile.profile(suffixedReadOnlyResourceName)),
            terraPath,
            resourceName,
            "READ_ONLY");
        validateCachedProfile(
            region,
            toResourceProfile(profileFile.profile(readOnlyResourceName)),
            awsVaultPath,
            suffixedReadOnlyResourceName);

        if (defaultResourceName.isPresent() && defaultResourceName.get().equals(resourceName)) {
          validateCachedProfile(
              region,
              toResourceProfile(profileFile.profile("default")),
              awsVaultPath,
              suffixedResourceName);
        }

      } else {
        validateResourceProfile(
            region,
            toResourceProfile(profileFile.profile(resourceName)),
            terraPath,
            resourceName,
            "WRITE_READ");
        validateResourceProfile(
            region,
            toResourceProfile(profileFile.profile(readOnlyResourceName)),
            terraPath,
            resourceName,
            "READ_ONLY");

        if (defaultResourceName.isPresent() && defaultResourceName.get().equals(resourceName)) {
          validateResourceProfile(
              region,
              toResourceProfile(profileFile.profile("default")),
              terraPath,
              resourceName,
              "WRITE_READ");
        }
      }
    }
  }
}
