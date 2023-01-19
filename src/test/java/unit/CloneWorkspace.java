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
public class CloneWorkspace extends ClearContextUnit {
  private static final TestUser workspaceCreator = TestUser.chooseTestUserWithSpendAccess();
  private static final Logger logger = LoggerFactory.getLogger(CloneWorkspace.class);

  @Test
  @DisplayName("clone workspace fails without new-id")
  public void cloneFailsWithoutNewUserFacingId() throws IOException {
    workspaceCreator.login();

    // `terra workspace clone`
    String stdErr = TestCommand.runCommandExpectExitCode(2, "workspace", "clone");
    assertThat(
        "error message indicate user must set ID",
        stdErr,
        CoreMatchers.containsString("Missing required option: '--new-id=<id>'"));
  }
}
