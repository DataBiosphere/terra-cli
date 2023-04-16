package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFAwsStorageFolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import java.util.List;
import java.util.stream.Collectors;

public class AwsStorageFolderUtils {
  /**
   * Utility method to verify the s3:// path of a storage folder format: "s3://%s/%s/" (last '/' is
   * optional).
   */
  public static boolean verifyS3Path(String s3Path, String prefix, boolean includesS3Prefix) {
    return s3Path.matches(
        String.format("^%s[a-zA-Z0-9_-]+/%s/?$", (includesS3Prefix ? "[sS]3://" : ""), prefix));
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  public static UFAwsStorageFolder listOneStorageFolderResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneStorageFolderResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  public static UFAwsStorageFolder listOneStorageFolderResourceWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFAwsStorageFolder> matchedResources =
        listStorageFolderResourcesWithName(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  public static List<UFAwsStorageFolder> listStorageFolderResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listStorageFolderResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  public static List<UFAwsStorageFolder> listStorageFolderResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=AWS_STORAGE_FOLDER --format=json`
    List<UFAwsStorageFolder> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=AWS_STORAGE_FOLDER")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=AWS_STORAGE_FOLDER",
                "--workspace=" + workspaceUserFacingId);

    // find the matching storage folder in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }

  public static void assertAwsStorageFolderFields(
      UFAwsStorageFolder expected, UFAwsStorageFolder actual, String src) {
    assertEquals(expected.name, actual.name, "storage folder name matches that in " + src);
    assertEquals(
        expected.bucketName, actual.bucketName, "storage folder bucketName matches that in " + src);
    assertEquals(expected.prefix, actual.prefix, "storage folder prefix matches that in " + src);
    assertEquals(expected.region, actual.region, "storage folder region matches that in " + src);
    assertEquals(
        expected.numObjects, actual.numObjects, "storage folder numObjects matches that in " + src);
  }
}
