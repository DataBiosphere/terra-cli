package bio.terra.cli.serialization.userfacing;

public class UFConfigItem {
  public final String option;
  public final String value;
  public final String description;

  /** Serialize an instance of the internal class to the command format. */
  public UFConfigItem(String option, String value, String description) {
    this.option = option;
    this.value = value;
    this.description = description;
  }
}
