package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.utils.HttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class ResourceUtils {
  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  public static <T extends UFResource> T listOneResourceWithName(
      String resourceName, Resource.Type resourceType) throws JsonProcessingException {
    return listOneResourceWithName(resourceName, resourceType, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  public static <T extends UFResource> T listOneResourceWithName(
      String resourceName, Resource.Type resourceType, String workspaceUserFacingId)
      throws JsonProcessingException {
    List<T> matchedResources =
        listResourcesWithName(resourceName, resourceType, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  public static <T extends UFResource> List<T> listResourcesWithName(
      String resourceName, Resource.Type resourceType) throws JsonProcessingException {
    return listResourcesWithName(resourceName, resourceType, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static <T extends UFResource> List<T> listResourcesWithName(
      String resourceName, Resource.Type resourceType, String workspaceUserFacingId)
      throws JsonProcessingException {
    // `terra resources list --type=<resourceType> --format=json`
    List<T> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=" + resourceType)
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=" + resourceType,
                "--workspace=" + workspaceUserFacingId);

    // find the matching notebook in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }

  /**
   * Helper method to poll `terra resources describe` until the resource field value equals that
   * specified. Uses the current workspace.
   */
  public static <T extends UFResource> void pollDescribeForResourceField(
      String resourceName, String fieldName, String fieldValue) throws InterruptedException {
    pollDescribeForResourceField(resourceName, fieldName, fieldValue, null);
  }

  /**
   * Helper method to poll `terra resources describe` until the resource field value equals that
   * specified. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static <T extends UFResource> void pollDescribeForResourceField(
      String resourceName, String fieldName, String fieldValue, String workspaceUserFacingId)
      throws InterruptedException {
    JSONObject resource =
        HttpUtils.pollWithRetries(
            () ->
                workspaceUserFacingId == null
                    ? TestCommand.runAndGetJsonObjectExpectSuccess(
                        "resource", "describe", "--name=" + resourceName)
                    : TestCommand.runAndGetJsonObjectExpectSuccess(
                        "resource",
                        "describe",
                        "--name=" + resourceName,
                        "--workspace=" + workspaceUserFacingId),
            (result) -> fieldValue.equals(result.get(fieldName)),
            (ex) -> false, // no retries
            60, // up to 60 minutes // TODO(BENCH-571): reduce this to 30 mins after this is fixed
            Duration.ofMinutes(1)); // every 1 minute

    assertNotNull(resource, "resource poll returned a resource");
    assertEquals(fieldValue, resource.get(fieldName), "resource field matches expected value");
  }
}
