package harness.utils;

public class ExternalAwsBuckets {
  /**
   * Utility method to verify the s3:// path of a bucket format: "s3://%s/%s/" (last '/' is
   * optional).
   */
  public static boolean verifyS3Path(String s3Path, String bucketPrefix, boolean includesS3Prefix) {
    return s3Path.matches(
        String.format(
            "^%s[a-zA-Z0-9_-]+/%s/?$", (includesS3Prefix ? "[sS]3://" : ""), bucketPrefix));
  }
}
