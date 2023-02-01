package harness.baseclasses;

import bio.terra.workspace.model.CloudPlatform;
import org.junit.jupiter.api.BeforeAll;

/** Base class for AWS unit tests that only need a single workspace for all test methods. */
public class SingleWorkspaceUnitAws extends SingleWorkspaceUnit {
  @BeforeAll
  protected void setupOnce() throws Exception {
    setCloudPlatform(CloudPlatform.AWS);
    super.setupOnce();
  }
}
