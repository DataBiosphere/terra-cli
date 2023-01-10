package harness.baseclasses;

import bio.terra.workspace.model.CloudPlatform;

/** Base class for AWS unit tests that only need a single workspace for all test methods. */
public class SingleWorkspaceUnitAws extends SingleWorkspaceUnit {
  @Override
  protected CloudPlatform getPlatform() {
    return CloudPlatform.AWS;
  }
}
