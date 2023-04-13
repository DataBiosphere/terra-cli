package bio.terra.cli.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.ini4j.Ini;

/**
 * Class to represent an AWS CLI/SDK profile for all of the AWS resources in a workspace, and write
 * it in <a href="https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html">the
 * expected format</a>.
 *
 * <p>This makes use of class {@link Ini}, which writes files in the ini format used by AWS config
 * files.
 */
public class AwsConfiguration {

  private final Map<String, Profile> profileMap;
  private final Optional<Profile> defaultProfile;

  private AwsConfiguration(Builder builder) {
    profileMap = new TreeMap<>(builder.profileMap);
    defaultProfile = builder.defaultProfile;
  }

  /** Gets a builder for the AwsConfiguration class. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Record type for specifying the 'region' and 'credential_process' configuration parameters for a
   * given AWS profile representing a single AWS resource.
   *
   * @param region region in which the resource exists
   * @param credentialProcess executable to execute in order to obtain a temporary credential to
   *     access the resource.
   */
  public static record Profile(String region, List<String> credentialProcess) {}

  /**
   * Factory method for creating {@link Profile} instances to pass to {@link Builder#addProfile}.
   *
   * @param region region of an AWS resource
   * @param credentialProcess command to call to obtain a credential for the resource
   * @return a {@link Profile} instance
   */
  public static Profile createProfile(String region, List<String> credentialProcess) {
    return new Profile(region, credentialProcess);
  }

  /**
   * Get the number of profiles in the configuration.
   *
   * @return the number of profiles in the configuration file
   */
  public int size() {
    return profileMap.size();
  }

  private void addProfileToSection(org.ini4j.Profile.Section section, Profile profile) {
    section.add("region", profile.region());
    section.add("credential_process", String.join(" ", profile.credentialProcess));
  }

  private void addDefaultProfile(Ini ini, Profile profile) {
    addProfileToSection(ini.add("default"), profile);
  }

  private void addNamedProfile(Ini ini, String name, Profile profile) {
    addProfileToSection(ini.add(String.format("profile %s", name)), profile);
  }

  @Override
  public String toString() {
    Ini ini = new Ini();

    defaultProfile.ifPresent(profile -> addDefaultProfile(ini, profile));

    for (Map.Entry<String, Profile> entry : profileMap.entrySet()) {
      addNamedProfile(ini, entry.getKey(), entry.getValue());
    }

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      ini.store(stream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new String(stream.toByteArray());
  }

  public static class Builder {

    private Builder() {
      profileMap = new HashMap();
      defaultProfile = Optional.empty();
    }

    private Map<String, Profile> profileMap;
    private Optional<Profile> defaultProfile;

    /**
     * Adds a profile to the configuration being built. Expects a {@link Profile} created with the
     * {@link #createProfile(String, List)} method.
     *
     * @param name
     * @param profile
     * @param isDefault
     * @return a reference to the Builder
     */
    public Builder addProfile(String name, Profile profile, boolean isDefault) {
      profileMap.put(name, profile);

      if (isDefault) {
        defaultProfile = Optional.of(profile);
      }

      return this;
    }

    /** Build the configuration */
    public AwsConfiguration build() {
      return new AwsConfiguration(this);
    }
  }
}
