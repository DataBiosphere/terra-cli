package unit;

import static bio.terra.cli.businessobject.Resource.Type.AWS_SAGEMAKER_NOTEBOOK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.AwsSageMakerNotebook;
import bio.terra.cli.serialization.userfacing.resource.UFAwsSageMakerNotebook;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.ResourceUtils;
import harness.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

/** Tests for the `terra resource` commands that handle controlled AWS SageMaker Notebooks. */
@Tag("unit-aws")
public class AwsSageMakerNotebookControlledTest extends SingleWorkspaceUnitAws {
  private static final String namePrefix = "cliTestAwsSageMaker";

  private static boolean verifySageMakerPath(
      String SageMakerPath, String instanceName, String region) {
    return SageMakerPath.equals(
        String.format(
            "https://%s.console.aws.amazon.com/sagemaker/home?region=%s#/notebook-instances/%s",
            region, region, instanceName));
  }

  private static void assertSageMakerNotebookFields(
      UFAwsSageMakerNotebook expected, UFAwsSageMakerNotebook actual, String src) {
    assertEquals(expected.name, actual.name, "notebook name matches that in " + src);
    assertEquals(
        expected.instanceName,
        actual.instanceName,
        "notebook instance name matches that in " + src);
    assertEquals(
        expected.instanceType,
        actual.instanceType,
        "notebook instance type matches that in " + src);
    assertEquals(
        expected.instanceStatus,
        actual.instanceStatus,
        "notebook instance status numObjects matches that in " + src);
  }

  @Test
  @DisplayName("list, describe and resolve reflect creating and deleting a controlled notebook")
  void listDescribeResolveReflectCreateDelete() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create sagemaker-notebook --name=$name --folder-name=folderName`
    String notebookName = TestUtils.appendRandomString(namePrefix);
    TestCommand.runCommandExpectSuccessWithRetries(
        "resource",
        "create",
        "sagemaker-notebook",
        "--name=" + notebookName,
        "--instance-name=" + notebookName,
        "--region=" + AWS_REGION);

    ResourceUtils.pollDescribeForResourceField(
        notebookName, "instanceStatus", NotebookInstanceStatus.IN_SERVICE.toString());

    // `terra resource describe --name=$name`
    UFAwsSageMakerNotebook createdResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsSageMakerNotebook.class, "resource", "describe", "--name=" + notebookName);

    // check the created resource has required details
    assertEquals(notebookName, createdResource.name, "created resource matches name");
    assertEquals(AWS_REGION, createdResource.region, "created resource matches region");
    assertEquals(
        notebookName, createdResource.instanceName, "created resource matches instance name");
    assertNotNull(createdResource.instanceType, "created resource returned instance type");
    assertNotNull(createdResource.instanceStatus, "created resource returned instance status");

    // sagemaker notebooks are always private, no clone support
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdResource.accessScope, "created resource matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdResource.privateUserName.toLowerCase(),
        "created resource matches private user name");
    assertEquals(
        CloningInstructionsEnum.NOTHING,
        createdResource.cloningInstructions,
        "created resource matches cloning instruction");

    // check that the notebook is in the resource list
    UFAwsSageMakerNotebook matchedResource =
        ResourceUtils.listOneResourceWithName(notebookName, AWS_SAGEMAKER_NOTEBOOK);
    assertSageMakerNotebookFields(createdResource, matchedResource, "list");

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + notebookName);
    assertTrue(
        verifySageMakerPath(String.valueOf(resolved.get(notebookName)), notebookName, AWS_REGION),
        "resolve matches expected path");

    // `terra resource credentials --name=$name --scope=READ_ONLY --duration=1500 --format=json`
    JSONObject resolvedCredentials =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource",
            "credentials",
            "--name=" + notebookName,
            "--scope=" + Resource.CredentialsAccessScope.READ_ONLY,
            "--duration=" + 900);
    assertNotNull(resolvedCredentials.get("Version"), "get credentials returned version");
    assertNotNull(resolvedCredentials.get("AccessKeyId"), "get credentials returned access key id");
    assertNotNull(
        resolvedCredentials.get("SecretAccessKey"), "get credentials returned access key");
    assertNotNull(
        resolvedCredentials.get("SessionToken"), "get credentials returned session token");
    assertNotNull(
        resolvedCredentials.get("Expiration"), "get credentials returned expiration date time");

    // `terra resource open-console --name=$name --scope=READ_ONLY --duration=1500`
    TestCommand.Result result =
        TestCommand.runCommandExpectSuccess(
            "resource",
            "open-console",
            "--name=" + notebookName,
            "--scope=" + Resource.CredentialsAccessScope.READ_ONLY,
            "--duration=" + 1500);
    assertThat(
        "console link is displayed",
        result.stdOut,
        containsString("Please open the following address in your browser"));
    assertThat(
        "console link is not opened in the browser",
        result.stdOut,
        not(containsString("Attempting to open that address in the default browser now...")));

    // `terra notebook launch --name=$name --format=json` // lab view
    JSONObject proxyUrl =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "notebook", "launch", "--name=" + notebookName);
    String url = proxyUrl.getString(notebookName);
    assertNotNull(url, "launch notebook returned proxy url for lab view");
    assertTrue(
        url.endsWith(AwsSageMakerNotebook.ProxyView.JUPYTERLAB.toParam()), "proxy url view is lab");

    // `terra notebook launch --name=$name --proxy-view=$proxyView --format=json` // lab view
    proxyUrl =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "notebook",
            "launch",
            "--name=" + notebookName,
            "--proxy-view=" + AwsSageMakerNotebook.ProxyView.JUPYTER);
    url = proxyUrl.getString(notebookName);
    assertNotNull(url, "launch notebook returned proxy url for classic view");
    assertTrue(
        url.endsWith(AwsSageMakerNotebook.ProxyView.JUPYTER.toParam()),
        "proxy url view is classic");

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "check-access", "--name=" + notebookName);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resource delete --name=$name`
    stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "delete", "--name=" + notebookName, "--quiet");
    assertThat(
        "error message includes expected and current statuses",
        stdErr,
        CoreMatchers.containsString("Expected notebook instance status is"));

    // `terra notebook stop --name=$name`
    result =
        TestCommand.runCommandExpectSuccessWithRetries(
            "notebook", "stop", "--name=" + notebookName);
    assertThat(
        "notebook successfully stopped",
        result.stdOut,
        containsString("Notebook instance stopped"));

    // `terra notebook start --name=$name`
    result =
        TestCommand.runCommandExpectSuccessWithRetries(
            "notebook", "start", "--name=" + notebookName);
    assertThat(
        "notebook start requested",
        result.stdOut,
        containsString(
            "Notebook instance starting. It may take a few minutes before it is available"));

    ResourceUtils.pollDescribeForResourceField(
        notebookName, "instanceStatus", NotebookInstanceStatus.IN_SERVICE.toString());

    result =
        TestCommand.runCommandExpectSuccessWithRetries(
            "notebook", "start", "--name=" + notebookName);
    assertThat(
        "notebook start request on a inService notebook returned immediately",
        result.stdOut,
        containsString("Notebook instance started"));

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "stop", "--name=" + notebookName);

    // `terra notebook launch --name=$name`
    stdErr =
        TestCommand.runCommandExpectExitCode(1, "notebook", "launch", "--name=" + notebookName);
    assertThat(
        "error message includes expected and current statuses",
        stdErr,
        CoreMatchers.containsString("Expected notebook instance status is"));

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + notebookName, "--quiet");

    // confirm it no longer appears in the resources list
    List<UFAwsSageMakerNotebook> listedBuckets =
        ResourceUtils.listResourcesWithName(notebookName, AWS_SAGEMAKER_NOTEBOOK);
    assertThat(
        "deleted notebook no longer appears in the resources list",
        listedBuckets,
        Matchers.empty());
  }
}
