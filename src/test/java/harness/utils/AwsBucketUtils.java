package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import java.util.List;
import java.util.stream.Collectors;

public class AwsBucketUtils {
  /**
   * Utility method to verify the s3:// path of a bucket format: "s3://%s/%s/" (last '/' is
   * optional).
   */
  public static boolean verifyS3Path(String s3Path, String bucketPrefix, boolean includesS3Prefix) {
    return s3Path.matches(
        String.format(
            "^%s[a-zA-Z0-9_-]+/%s/?$", (includesS3Prefix ? "[sS]3://" : ""), bucketPrefix));
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  public static UFAwsBucket listOneBucketResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneBucketResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  public static UFAwsBucket listOneBucketResourceWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFAwsBucket> matchedResources =
        listBucketResourcesWithName(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  public static List<UFAwsBucket> listBucketResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listBucketResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  public static List<UFAwsBucket> listBucketResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=AWS_BUCKET --format=json`
    List<UFAwsBucket> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=AWS_BUCKET")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=AWS_BUCKET",
                "--workspace=" + workspaceUserFacingId);

    // find the matching bucket in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
