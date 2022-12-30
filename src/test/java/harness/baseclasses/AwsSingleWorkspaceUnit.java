package harness.baseclasses;

import bio.terra.workspace.model.CloudPlatform;
import java.util.Optional;

/** Base class for AWS unit tests that only need a single workspace for all test methods. */
public class AwsSingleWorkspaceUnit extends SingleWorkspaceUnit {
  @Override
  protected Optional<CloudPlatform> getPlatform() {
    return Optional.of(CloudPlatform.AWS);
  }
}
