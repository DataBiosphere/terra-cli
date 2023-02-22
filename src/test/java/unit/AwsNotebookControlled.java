package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.businessobject.resource.AwsNotebook;
import bio.terra.cli.serialization.userfacing.resource.UFAwsNotebook;
import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.AwsNotebookUtils;
import harness.utils.GcpNotebookUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

/** Tests for the `terra resource` commands that handle controlled Aws notebooks. */
@Tag("unit-aws")
public class AwsNotebookControlled extends SingleWorkspaceUnitAws {
  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  public static UFAwsNotebook listOneNotebookResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneNotebookResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  public static UFAwsNotebook listOneNotebookResourceWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFAwsNotebook> matchedResources =
        listNotebookResourcesWithName(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  public static List<UFAwsNotebook> listNotebookResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listNotebookResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static List<UFAwsNotebook> listNotebookResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=AWS_SAGEMAKER_NOTEBOOK --format=json`
    List<UFAwsNotebook> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=AWS_SAGEMAKER_NOTEBOOK")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=AWS_SAGEMAKER_NOTEBOOK",
                "--workspace=" + workspaceUserFacingId);

    // find the matching notebook in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }

  @Test
  @DisplayName(
      "list, describe and resolve reflect creating, stopping, starting and deleting a controlled AWS notebook")
  void listDescribeResolveReflectCreateStopStartDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create aws-notebook --name=$name`
    String resourceName = UUID.randomUUID().toString();
    UFAwsNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsNotebook.class, "resource", "create", "aws-notebook", "--name=" + resourceName);

    // check that the name and notebook name match
    assertEquals(resourceName, createdNotebook.name, "create output matches name");
    assertNotNull(createdNotebook.instanceId, "create resource output includes instance id");
    assertEquals(
        NotebookInstanceStatus.IN_SERVICE.toString(),
        createdNotebook.state,
        "create output state matches initial state");
    // TODO(TERRA-228) Support notebook creation parameters
    // assertEquals("bar", createdNotebook.metadata.get("foo"), "create output matches metadata");

    // aws notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdNotebook.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdNotebook.privateUserName.toLowerCase(),
        "create output matches private user name");

    // check that the notebook is in the list
    UFAwsNotebook matchedResource = listOneNotebookResourceWithName(resourceName);
    assertEquals(resourceName, matchedResource.name, "list output matches name");
    assertNotNull(matchedResource.instanceId, "list resource output includes instance id");
    assertEquals(resourceName, matchedResource.name, "list output state matches initial state");

    // `terra resource describe --name=$name --format=json`
    UFAwsNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsNotebook.class, "resource", "describe", "--name=" + resourceName);

    // check that the name matches and the instance id is populated
    assertEquals(resourceName, describeResource.name, "describe resource output matches name");
    assertNotNull(describeResource.instanceId, "describe resource output includes instance id");
    assertEquals(resourceName, describeResource.name, "list output state matches initial state");

    // aws notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "stop", "--name=" + resourceName);
    AwsNotebookUtils.assertNotebookState(resourceName, NotebookInstanceStatus.STOPPED);

    // `terra notebook start --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "start", "--name=" + resourceName);
    AwsNotebookUtils.assertNotebookState(resourceName, NotebookInstanceStatus.IN_SERVICE);

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName);
    assertEquals(
        AwsNotebook.resolve(createdNotebook.location, createdNotebook.instanceId, true),
        resolved.get(resourceName),
        "resolve returns the instance name");

    // `terra resource check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "check-access", "--name=" + resourceName);
    assertThat(
        "check-access error because aws notebooks are controlled resources",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resource delete --name=$name`
    stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "delete", "--name=" + resourceName);
    assertThat(
        "delete error because aws notebooks must be stopped before deletion",
        stdErr,
        CoreMatchers.containsString(
            "delete error because aws notebooks must be stopped before deletion"));
    // TODO-Dex

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "stop", "--name=" + resourceName);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries(
        "resource", "delete", "--name=" + resourceName, "--quiet");

    // confirm it no longer appears in the resources list
    List<UFGcpNotebook> listedNotebooks =
        GcpNotebookUtils.listNotebookResourcesWithName(resourceName);
    assertThat(
        "deleted notebook no longer appears in the resources list",
        listedNotebooks,
        Matchers.empty());
  }

  @Test
  @DisplayName("override the default location and instance id of a controlled AWS notebook")
  void overrideLocationAndInstanceId() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create aws-notebook --name=$name
    // --cloning=$cloning --description=$description
    // --location=$location --instance-id=$instanceId`
    String resourceName = UUID.randomUUID().toString();
    CloningInstructionsEnum cloning = CloningInstructionsEnum.RESOURCE;
    String description = "\"override default location and instance id\"";
    String location = "us-east-1";
    // TODO(TERRA-371) Support instance-id
    // String instanceId = "a" + UUID.randomUUID(); // instance id must start with a letter
    UFAwsNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsNotebook.class,
            "resource",
            "create",
            "aws-notebook",
            "--name=" + resourceName,
            "--cloning=" + cloning,
            "--description=" + description,
            "--location=" + location);

    // check that the properties match
    assertEquals(resourceName, createdNotebook.name, "create output matches name");
    assertEquals(cloning, createdNotebook.cloningInstructions, "create output matches cloning");
    assertEquals(description, createdNotebook.description, "create output matches description");
    assertEquals(location, createdNotebook.location, "create output matches location");
    // assertEquals(instanceId, createdNotebook.instanceId, "create output matches instance id");

    // aws notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdNotebook.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdNotebook.privateUserName.toLowerCase(),
        "create output matches private user name");

    // `terra resource describe --name=$name --format=json`
    UFAwsNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsNotebook.class, "resource", "describe", "--name=" + resourceName);

    // check that the properties match
    assertEquals(resourceName, describeResource.name, "describe resource output matches name");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");
    assertEquals(location, describeResource.location, "describe resource output matches location");
    // assertEquals(instanceId, describeResource.instanceId, "describe resource output matches
    // instance id");

    // aws notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");

    /* TODO(TERRA-218) Support Notebook update
    // new key-value pair will be appended, existing key-value pair will be updated.
    String newResourceName = UUID.randomUUID().toString();
    String newDescription = "\"new override default location and instance id\"";
    UFAwsNotebook updatedNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsNotebook.class,
            "resource",
            "update",
            "aws-notebook",
            "--name=" + resourceName,
            "--new-name=" + newResourceName,
            "--new-description=" + newDescription);

    // check that the properties match
    // the metadata supports multiple entries, we can't assert on the metadata because it's not
    // stored in or accessible via Workspace Manager.
    assertEquals(newResourceName, updatedNotebook.name, "create output matches name");
    assertEquals(newDescription, updatedNotebook.description, "create output matches description");
     */
    // TODO(TERRA-228) Support notebook creation parameters
    /* assertEquals(
        newValue1,
        updatedNotebook.metadata.get(newKey1),
        "create output matches metadata" + newKey1 + ": " + newValue1);
    assertEquals(
        newValue2,
        updatedNotebook.metadata.get(newKey2),
        "create output matches metadata" + newKey2 + ": " + newValue2);
     */
  }
}
