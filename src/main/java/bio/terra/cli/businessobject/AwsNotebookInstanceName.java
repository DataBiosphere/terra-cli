package bio.terra.cli.businessobject;

import com.google.auto.value.AutoValue;
import com.google.gson.JsonObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convenience class for formatting the project/location/instanceId of a notebooks instance for
 * {@link com.google.api.services.notebooks.v1.AIPlatformNotebooks.Projects.Locations.Instances}.
 */
@AutoValue
public abstract class AwsNotebookInstanceName {
  private static final Pattern NAME_PATTERN =
      Pattern.compile("^projects/([^/]+)/locations/([^/]+)/instances/([^/]+)$");
  private static final Pattern PARENT_PATTERN =
      Pattern.compile("^projects/([^/]+)/locations/([^/]+)$");

  /** The AWS Account Number that contains this instance. */
  public abstract String awsAccountNumber();
  /** The landing zone id that contains this instance. */
  public abstract String landingZoneId();
  /** The location where the instance is, e.g. "us-west1-b". */
  public abstract String location();
  /** The user specified id for the instance. */
  public abstract String instanceId();

  /**
   * Returns a String for the name of an instance with this parent in the format expected by {@link
   * com.google.api.services.notebooks.v1.AIPlatformNotebooks.Projects.Locations.Instances}
   * functions.
   */
  public String formatName() {
    return "";
    // return String.format(
    //   "projects/%s/locations/%s/instances/%s", projectId(), location(), instanceId());
  }

  /**
   * Returns a String for the parent of this instance in the parent format expected by {@link // *
   * com.google.api.services.notebooks.v1.AIPlatformNotebooks.Projects.Locations.Instances#create(String,
   * // * Instance)}.
   */
  public String formatParent() {
    return "";
    // return String.format("projects/%s/locations/%s", projectId(), location());
  }

  /**
   * Parse the name format to create an {@link AwsNotebookInstanceName}.
   *
   * @throws IllegalArgumentException on parse failure.
   * @see #formatName()
   */
  public static AwsNotebookInstanceName fromNameFormat(String name) {
    Matcher matcher = NAME_PATTERN.matcher(name);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          String.format("Name must conform to %s but got '%s'", NAME_PATTERN.pattern(), name));
    }
    return AwsNotebookInstanceName.builder()
        .awsAccountNumber(matcher.group(1))
        .landingZoneId(matcher.group(1))
        .location(matcher.group(2))
        .instanceId(matcher.group(3))
        .build();
  }

  /**
   * Parse the parent format with an instance ID to create an {@link AwsNotebookInstanceName}.
   *
   * @throws IllegalArgumentException on parse failure.
   * @see #formatParent()
   */
  public static AwsNotebookInstanceName fromParentAndId(String parent, String instanceId) {
    Matcher matcher = PARENT_PATTERN.matcher(parent);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          String.format(
              "PARENT must conform to %s but got '%s'", PARENT_PATTERN.pattern(), parent));
    }
    return AwsNotebookInstanceName.builder()
        .awsAccountNumber(matcher.group(1))
        .landingZoneId(matcher.group(1))
        .location(matcher.group(2))
        .instanceId(instanceId)
        .build();
  }

  public static Builder builder() {
    return null;
    // return new bio.terra.cloudres.aws.notebooks.AutoValue_AwsNotebookInstanceName.Builder();
  }

  /** Adds properties to the JsonObject for the fields on this. */
  void addProperties(JsonObject jsonObject) {
    jsonObject.addProperty("awsAccountNumber", awsAccountNumber());
    jsonObject.addProperty("landingZoneId", landingZoneId());
    jsonObject.addProperty("locations", location());
    jsonObject.addProperty("instanceId", instanceId());
  }

  /** Builder for {@link AwsNotebookInstanceName}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder awsAccountNumber(String awsAccountNumber);

    public abstract Builder landingZoneId(String landingZoneId);

    public abstract Builder location(String location);

    public abstract Builder instanceId(String instanceId);

    public abstract AwsNotebookInstanceName build();
  }
}
