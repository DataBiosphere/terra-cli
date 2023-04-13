package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.command.workspace.ConfigureAws;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;

public class AwsConfigurationUtils {
  private static Pattern configOutputPattern = Pattern.compile("export (.*)=(.*)");

  public static Path getProfilePathFromOutput(String configOutput) {
    Matcher matcher = configOutputPattern.matcher(configOutput);
    assertTrue(matcher.find());
    assertEquals(2, matcher.groupCount());
    assertEquals(ConfigureAws.AWS_CONFIG_FILE_ENVIRONMENT_VARIABLE, matcher.group(1));
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
      Optional<String> optionalTerraPath,
      String resourceName,
      String access) {
    String terraPath = optionalTerraPath.orElse(ConfigureAws.DEFAULT_TERRA_PATH);
    String expectedCommandLine =
        String.join(" ", ConfigureAws.buildResourceCommandLine(terraPath, resourceName, access));
    assertEquals(expectedCommandLine, resourceProfile.credentialProcess());
    assertEquals(region, resourceProfile.region());
  }

  private static void validateCachedProfile(
      String region,
      ResourceProfile resourceProfile,
      Optional<String> optionalAwsVaultPath,
      String targetProfileName) {
    String awsVaultPath = optionalAwsVaultPath.orElse(ConfigureAws.DEFAULT_AWS_VAULT_PATH);
    String expectedCommandLine =
        String.join(" ", ConfigureAws.buildCachingCommandLine(awsVaultPath, targetProfileName));
    assertEquals(expectedCommandLine, resourceProfile.credentialProcess());
    assertEquals(region, resourceProfile.region());
  }

  public static void validateConfiguration(
      String configOutput,
      String region,
      Collection<String> resourceNames,
      Optional<String> defaultResourceName,
      boolean cacheWithAwsVault,
      Optional<String> terraPath,
      Optional<String> awsVaultPath) {
    ProfileFile profileFile =
        ProfileFile.builder()
            .type(ProfileFile.Type.CONFIGURATION)
            .content(getProfilePathFromOutput(configOutput))
            .build();

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
