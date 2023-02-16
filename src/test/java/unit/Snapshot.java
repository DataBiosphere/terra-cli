package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.service.utils.CrlUtils;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra snapshot` commands. */
@Tag("unit")
public class Snapshot extends SingleWorkspaceUnit {
  @Test
  @DisplayName("snapshot")
  void cromwellGenerateConfig() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // Create a dummy file to snapshot
    TestCommand.runCommandExpectSuccess("touch", "foo.ipynb");

    // `terra snapshot --filePath=foo.ipynb --comment="foo comment"`
    TestCommand.runCommandExpectSuccess(
        "snapshot", "--filePath=foo.ipynb", "--comment=\"foo comment\"");

    // Poll until we can read the number of objects in the bucket.
    UFGcsBucket SnapshotsBucket =
        CrlUtils.callGcpWithPermissionExceptionRetries(
            () ->
                TestCommand.runAndParseCommandExpectSuccess(
                    UFGcsBucket.class, "resource", "describe", "--name=snapshots"),
            gcsBucket -> gcsBucket.numObjects != null);

    // Check that there is now 2 objects reported in the bucket
    // One for the snapshot and one for the comment
    assertEquals(2, SnapshotsBucket.numObjects, "snapshot bucket contains 1 object");
  }
}
