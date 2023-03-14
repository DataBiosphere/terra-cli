package unit;

import static org.hamcrest.MatcherAssert.assertThat;

import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import java.io.IOException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
public class DuplicateWorkspace extends ClearContextUnit {
  private static final TestUser workspaceCreator = TestUser.chooseTestUserWithSpendAccess();

  @Test
  @DisplayName("duplicate workspace fails without new-id")
  public void duplicateFailsWithoutNewUserFacingId() throws IOException {
    workspaceCreator.login();

    // `terra workspace clone`
    String stdErr = TestCommand.runCommandExpectExitCode(2, "workspace", "duplicate");
    assertThat(
        "error message indicate user must set ID",
        stdErr,
        CoreMatchers.containsString("Missing required option: '--new-id=<id>'"));
  }
}
