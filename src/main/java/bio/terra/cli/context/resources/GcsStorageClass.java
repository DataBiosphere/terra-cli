package bio.terra.cli.context.resources;

import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;

/**
 * This enum defines the possible storage classes for buckets, and maps these classes to the
 * corresponding enum in the WSM client library.
 *
 * <p>This class mirrors the {@link GcpGcsBucketDefaultStorageClass} class in the WSM client
 * library. The CLI defines its own version of this enum instead of using the WSM client library
 * version so that:
 *
 * <p>- The CLI syntax does not change when WSM API changes. In this case, the syntax affected is
 * the structure of the user-provided JSON file to specify a lifecycle fule.
 *
 * <p>- The CLI can more easily control the JSON mapping behavior of the enum. In this case, the WSM
 * client library version of the enum provides a @JsonCreator fromValue method that is case
 * sensitive, and the CLI wants to allow case insensitive deserialization.
 */
public enum GcsStorageClass {
  STANDARD(GcpGcsBucketDefaultStorageClass.STANDARD),
  NEARLINE(GcpGcsBucketDefaultStorageClass.NEARLINE),
  COLDLINE(GcpGcsBucketDefaultStorageClass.COLDLINE),
  ARCHIVE(GcpGcsBucketDefaultStorageClass.ARCHIVE);

  private GcpGcsBucketDefaultStorageClass wsmEnumVal;

  GcsStorageClass(GcpGcsBucketDefaultStorageClass wsmEnumVal) {
    this.wsmEnumVal = wsmEnumVal;
  }

  public GcpGcsBucketDefaultStorageClass toWSMEnum() {
    return this.wsmEnumVal;
  }
}
