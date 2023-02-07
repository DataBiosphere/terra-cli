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
  public static UFAwsBucket listOneBucketResourceWithNameAws(String resourceName)
          throws JsonProcessingException {
    return listOneBucketResourceWithNameAws(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  static UFAwsBucket listOneBucketResourceWithNameAws(
          String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFAwsBucket> matchedResources =
            listBucketResourcesWithNameAws(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  static List<UFAwsBucket> listBucketResourcesWithNameAws(String resourceName)
          throws JsonProcessingException {
    return listBucketResourcesWithNameAws(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  static List<UFAwsBucket> listBucketResourcesWithNameAws(
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
