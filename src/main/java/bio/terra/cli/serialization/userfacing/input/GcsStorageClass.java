package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;

/**
 * This enum defines the possible storage classes for buckets, and maps these classes to the
 * corresponding {@link GcpGcsBucketDefaultStorageClass} enum in the WSM client library. The CLI
 * defines its own version of this enum so that:
 *
 * <p>- The CLI syntax does not change when WSM API changes. In this case, the syntax affected is
 * the structure of the user-provided JSON file to specify a lifecycle rule.
 *
 * <p>- The CLI can more easily control the JSON mapping behavior of the enum. In this case, the WSM
 * client library version of the enum provides a @JsonCreator fromValue method that is case
 * sensitive, and the CLI may want to allow case insensitive deserialization.
 */
public enum GcsStorageClass {
  STANDARD(GcpGcsBucketDefaultStorageClass.STANDARD),
  NEARLINE(GcpGcsBucketDefaultStorageClass.NEARLINE),
  COLDLINE(GcpGcsBucketDefaultStorageClass.COLDLINE),
  ARCHIVE(GcpGcsBucketDefaultStorageClass.ARCHIVE);

  private final GcpGcsBucketDefaultStorageClass wsmEnumVal;

  GcsStorageClass(GcpGcsBucketDefaultStorageClass wsmEnumVal) {
    this.wsmEnumVal = wsmEnumVal;
  }

  public GcpGcsBucketDefaultStorageClass toWSMEnum() {
    return this.wsmEnumVal;
  }
}
