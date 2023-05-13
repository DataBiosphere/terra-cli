package unit;

import static bio.terra.cli.businessobject.Resource.Type.AWS_SAGEMAKER_NOTEBOOK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.userfacing.resource.UFAwsSageMakerNotebook;
import bio.terra.workspace.model.AccessScope;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.ResourceUtils;
import harness.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AWS SageMaker Notebooks. */
@Tag("unit-aws")
public class AwsSageMakerNotebookControlled extends SingleWorkspaceUnitAws {
  private static boolean verifySageMakerPath(
      String SageMakerPath, String instanceName, String region) {
    return SageMakerPath.matches(
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
  void listDescribeResolveReflectCreateDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create sagemaker-notebook --name=$name --folder-name=folderName`
    String instanceName = UUID.randomUUID().toString();
    String name = "listDescribeResolveReflectCreateDelete" + instanceName;
    UFAwsSageMakerNotebook createdResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsSageMakerNotebook.class,
            "resource",
            "create",
            "sagemaker-notebook",
            "--name=" + name,
            "--instance-name=" + instanceName,
            "--region=" + AWS_REGION);

    // check the created resource has required details
    assertEquals(name, createdResource.name, "created resource matches name");
    assertEquals(AWS_REGION, createdResource.region, "created resource matches region");
    assertEquals(
        instanceName, createdResource.instanceName, "created resource matches instance name");
    assertEquals(
        instanceName, createdResource.instanceType, "created resource matches instance type");
    assertNotNull(createdResource.instanceStatus, "created resource returned instance status");

    // sagemaker notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdResource.accessScope, "created resource matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdResource.privateUserName.toLowerCase(),
        "created resource matches private user name");

    // check that the notebook is in the resource list
    UFAwsSageMakerNotebook matchedResource =
        ResourceUtils.listOneResourceWithName(name, AWS_SAGEMAKER_NOTEBOOK);
    assertSageMakerNotebookFields(createdResource, matchedResource, "list");

    // `terra resource describe --name=$name --format=json`
    UFAwsSageMakerNotebook describedResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsSageMakerNotebook.class, "resource", "describe", "--name=" + name);

    // check the new notebook is returned by describe
    TestUtils.assertResourceProperties(createdResource, describedResource, "describe");
    assertSageMakerNotebookFields(createdResource, describedResource, "describe");

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + name);
    assertTrue(
        verifySageMakerPath(String.valueOf(resolved.get(name)), instanceName, AWS_REGION),
        "resolve matches expected path");

    // `terra resource credentials --name=$name --scope=READ_ONLY --duration=1500 --format=json`
    JSONObject resolvedCredentials =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource",
            "credentials",
            "--name=" + name,
            "--scope=" + Resource.CredentialsAccessScope.READ_ONLY,
            "--duration=" + 1500);
    assertNotNull(resolvedCredentials.get("Version"), "get credentials returned version");
    assertNotNull(resolvedCredentials.get("AccessKeyId"), "get credentials returned access key id");
    assertNotNull(
        resolvedCredentials.get("SecretAccessKey"), "get credentials returned access key");
    assertNotNull(
        resolvedCredentials.get("SessionToken"), "get credentials returned session token");
    assertNotNull(
        resolvedCredentials.get("Expiration"), "get credentials returned expiration date time");

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resource delete --name=$name`
    stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "delete", "--name=" + name, "--quiet");
    assertThat(
        "Expected notebook instance status is [Failed, Stopped] but current status is InService",
        stdErr,
        CoreMatchers.containsString(
            "Expected notebook instance status is [Failed, Stopped] but current status is InService"));

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "stop", "--name=" + name);

    // `terra notebook start --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "start", "--name=" + name);

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "stop", "--name=" + name);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // confirm it no longer appears in the resources list
    List<UFAwsSageMakerNotebook> listedBuckets =
        ResourceUtils.listResourcesWithName(name, AWS_SAGEMAKER_NOTEBOOK);
    assertThat(
        "deleted notebook no longer appears in the resources list",
        listedBuckets,
        Matchers.empty());
  }
}
