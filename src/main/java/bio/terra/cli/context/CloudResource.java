package bio.terra.cli.context;

import bio.terra.cli.service.utils.GoogleCloudStorage;

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

  // default constructor required for Jackson de/serialization
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

    // true means this cloud resource will be included in the list of data references for a
    // workspace
    public final boolean isDataReference;

    Type(boolean isDataReference) {
      this.isDataReference = isDataReference;
    }
  }

  /**
   * Check whether the user has access to this cloud resource.
   *
   * @param terraUser the user whose credentials we use to do the check
   * @return true if the user has access
   */
  public boolean checkAccessForUser(TerraUser terraUser) {
    return new GoogleCloudStorage(terraUser.userCredentials).checkAccess(cloudId);
  }

  /**
   * Check whether the user's pet SA has access to this cloud resource.
   *
   * @param terraUser the user whose pet SA credentials we use to do the check
   * @return true if the user's pet SA has access
   */
  public boolean checkAccessForPetSa(TerraUser terraUser) {
    return new GoogleCloudStorage(terraUser.petSACredentials).checkAccess(cloudId);
  }
}
