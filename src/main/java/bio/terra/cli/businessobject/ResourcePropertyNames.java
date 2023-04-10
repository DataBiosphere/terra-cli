package bio.terra.cli.businessobject;

public enum ResourcePropertyNames {
  FolderId("terra-folder-id");

  private final String value;

  ResourcePropertyNames(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
