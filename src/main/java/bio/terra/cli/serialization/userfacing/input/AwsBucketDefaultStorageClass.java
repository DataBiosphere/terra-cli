package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// TODO(TERRA-203) Move this to WSM client
public enum AwsBucketDefaultStorageClass {
  STANDARD("STANDARD"),
  INTELLIGENT_TIERING("INTELLIGENT_TIERING"),
  STANDARD_INFREQUENT_ACCESS("STANDARD_INFREQUENT_ACCESS"),
  ONE_ZONE_INFREQUENT_ACCESS("ONE_ZONE_INFREQUENT_ACCESS"),
  GLACIER_INSTANT_RETRIEVAL("GLACIER_INSTANT_RETRIEVAL"),
  GLACIER_FLEXIBLE_RETRIEVAL("GLACIER_FLEXIBLE_RETRIEVAL"),
  GLACIER_DEEP_ARCHIVE("GLACIER_DEEP_ARCHIVE");

  private String value;

  private AwsBucketDefaultStorageClass(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return this.value;
  }

  public String toString() {
    return String.valueOf(this.value);
  }

  @JsonCreator
  public static AwsBucketDefaultStorageClass fromValue(String text) {
    AwsBucketDefaultStorageClass[] var1 = values();
    int var2 = var1.length;

    for (int var3 = 0; var3 < var2; ++var3) {
      AwsBucketDefaultStorageClass b = var1[var3];
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }

    return null;
  }
}
