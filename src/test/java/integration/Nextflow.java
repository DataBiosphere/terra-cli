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

@Tag("integration-gcp")
public class Nextflow extends ClearContextIntegration {
  @Test
  @DisplayName("nextflow run hello")
  void helloWorld() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login(/*writeGcloudAuthFiles=*/ true);

    // create a workspace
    int exitCode = TestBashScript.runScript("CreateWorkspace.sh");
    assertEquals(0, exitCode, "workspace created without errors");

    // run the script that runs the NF hello world workflow
    exitCode = TestBashScript.runScript("NextflowHelloWorld.sh");

    // check that the NF script ran successfully
    assertEquals(0, exitCode, "script completed without errors");

    // check that the NF output includes the "Hello world!" string that indicates the workflow ran
    String scriptOutput =
        Files.readString(
            TestBashScript.getOutputFilePath("nextflowHelloWorld_stdout.txt"),
            StandardCharsets.UTF_8);
    assertThat(
        "nextflow output includes the hello world string",
        scriptOutput,
        CoreMatchers.containsString("Hello world!"));
  }
}
