package bio.terra.cli.context;

/** This POJO class represents a Terra workspace cloud resource (controlled or external). */
public class CloudResource {
  // name of the cloud resource. names are unique within a workspace
  public String name;

  // cloud identifier for the resource (e.g. bucket uri, bq dataset id)
  public String cloudId;

  // type of resource (e.g. bucket, bq dataset, vm)
  public Type type;

  // true = this cloud resource maps to a controlled resource within the workspace
  // false = this cloud resource maps to an external resource within the workspace
  public boolean isControlled;

  public CloudResource() {}

  public CloudResource(String name, String cloudId, Type type, boolean isControlled) {
    this.name = name;
    this.cloudId = cloudId;
    this.type = type;
    this.isControlled = isControlled;
  }

  /** Type of cloud resource. */
  public enum Type {
    bucket(true);

    // true = this cloud resource is also a data reference
    public final boolean isDataReference;

    Type(boolean isDataReference) {
      this.isDataReference = isDataReference;
    }
  }
}
