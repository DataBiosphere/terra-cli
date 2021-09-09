package unit;

import bio.terra.cli.serialization.userfacing.UFClonedWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.ClearContextUnit;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@Tag("unit")
public class CloneWorkspace extends ClearContextUnit {
  protected static final TestUsers workspaceCreator = TestUsers.chooseTestUserWithSpendAccess();

  @BeforeEach
  @Override
  public void setupEachTime() throws IOException {
    super.setupEachTime();
  }

  @Test
  public void cloneWorkspace(TestInfo testInfo) throws Exception {
    workspaceCreator.login();

    // create a workspace
    // `terra workspace create --format=json`
    UFWorkspace sourceWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // Switch to the new workspace
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + sourceWorkspace.id);

    // Add a bucket resource
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "gcs-bucket",
        "--name=" + "bucket_1",
        "--bucket-name=" + UUID.randomUUID());

    // Add a dataset resource
    // Add a referenced resource

    // Clone the workspace
    UFClonedWorkspace clonedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFClonedWorkspace.class,
            "workspace",
            "clone",
            "--name=" + "cloned_workspace",
            "--description=" + "\"A clone.\"");

    // Switch to the new workspace from the clone
    // Validate resources
  }
}
