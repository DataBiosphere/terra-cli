package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.AwsBucketDefaultStorageClass;

/**
 * This enum defines the possible storage classes for buckets, and maps these classes to the
 * corresponding {@link AwsBucketDefaultStorageClass} enum in the WSM client library. The CLI
 * defines its own version of this enum so that:
 *
 * <p>- The CLI syntax does not change when WSM API changes. In this case, the syntax affected is
 * the structure of the user-provided JSON file to specify a lifecycle rule.
 *
 * <p>- The CLI can more easily control the JSON mapping behavior of the enum. In this case, the WSM
 * client library version of the enum provides a @JsonCreator fromValue method that is case
 * sensitive, and the CLI may want to allow case insensitive deserialization.
 */
public enum AwsStorageClass {
  STANDARD(AwsBucketDefaultStorageClass.STANDARD),
  INTELLIGENT_TIERING(AwsBucketDefaultStorageClass.INTELLIGENT_TIERING),
  STANDARD_INFREQUENT_ACCESS(AwsBucketDefaultStorageClass.STANDARD_INFREQUENT_ACCESS),
  ONE_ZONE_INFREQUENT_ACCESS(AwsBucketDefaultStorageClass.ONE_ZONE_INFREQUENT_ACCESS),
  GLACIER_INSTANT_RETRIEVAL(AwsBucketDefaultStorageClass.GLACIER_INSTANT_RETRIEVAL),
  GLACIER_FLEXIBLE_RETRIEVAL(AwsBucketDefaultStorageClass.GLACIER_FLEXIBLE_RETRIEVAL),
  GLACIER_DEEP_ARCHIVE(AwsBucketDefaultStorageClass.GLACIER_DEEP_ARCHIVE);

  private AwsBucketDefaultStorageClass wsmEnumVal;

  AwsStorageClass(AwsBucketDefaultStorageClass wsmEnumVal) {
    this.wsmEnumVal = wsmEnumVal;
  }

  public AwsBucketDefaultStorageClass toWSMEnum() {
    return this.wsmEnumVal;
  }
}
