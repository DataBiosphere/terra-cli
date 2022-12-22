package integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import harness.TestBashScript;
import harness.TestUser;
import harness.baseclasses.ClearContextIntegration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class Notebook extends ClearContextIntegration {
  @Test
  @DisplayName("post startup script sets correct env vars")
  void notebookPostStartupScript() throws IOException {
    // Select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login(/*writeGcloudAuthFiles=*/ true);

    // Create a workspace
    int exitCode = TestBashScript.runScript("CreateWorkspace.sh");
    assertEquals(0, exitCode, "workspace created without errors");

    // Create a notebook and get the environment variables
    exitCode = TestBashScript.runScript("NotebookPostStartup.sh");
    assertEquals(0, exitCode, "notebook created without errors");

    String scriptOutput =
        Files.readString(
            TestBashScript.getOutputFilePath("notebookPostStartupScript_stdout.txt"),
            StandardCharsets.UTF_8);

    String[] envVars = {
      "GOOGLE_CLOUD_PROJECT",
      "OWNER_EMAIL",
      "TERRA_USER_EMAIL",
      "GGOGLE_PROJECT",
      "CROMWELL_JAR",
      "GOOGLE_SERVICE_ACCOUNT_EMAIL",
      "PET_SA_EMAIL"
    };

    // Check that all expected env variables are present
    for (String var : envVars) {
      assertThat(
          "output includes the env variable " + var,
          scriptOutput,
          CoreMatchers.containsString(var));
    }
  }
}
