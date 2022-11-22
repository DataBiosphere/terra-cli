package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// TODO(TERRA-203) Move this to WSM client
public enum AwsBucketLifecycleRuleActionType {
  DELETE("DELETE"),
  SET_STORAGE_CLASS("SET_STORAGE_CLASS");

  private String value;

  private AwsBucketLifecycleRuleActionType(String value) {
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
  public static AwsBucketLifecycleRuleActionType fromValue(String text) {
    AwsBucketLifecycleRuleActionType[] var1 = values();
    int var2 = var1.length;

    for (int var3 = 0; var3 < var2; ++var3) {
      AwsBucketLifecycleRuleActionType b = var1[var3];
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }

    return null;
  }
}
