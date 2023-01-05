package harness.baseclasses;

import bio.terra.workspace.model.CloudPlatform;
import java.util.Optional;

/** Base class for GCP unit tests that only need a single workspace for all test methods. */
public class SingleWorkspaceUnitGcp extends SingleWorkspaceUnit {
  @Override
  protected Optional<CloudPlatform> getPlatform() {
    return Optional.of(CloudPlatform.GCP);
  }
}
