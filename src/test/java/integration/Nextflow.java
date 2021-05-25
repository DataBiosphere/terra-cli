package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import harness.TestBashScript;
import harness.TestUsers;
import harness.baseclasses.ClearContextIntegration;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class Nextflow extends ClearContextIntegration {

  @Test
  @DisplayName("nextflow config and run with GLS tutorial")
  void nextflowFromGLSTutorial() throws IOException, InterruptedException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUserWithSpendAccess();
    testUser.login();

    // run the script that downloads the NF workflow from GH and runs it
    int exitCode = new TestBashScript().runScript("NextflowRnaseq.sh");

    // check that the NF script ran successfully
    assertEquals(0, exitCode, "script completed without errors");
  }
}
