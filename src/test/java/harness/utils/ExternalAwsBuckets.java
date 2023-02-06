package harness.utils;

public class ExternalAwsBuckets {

  /** Utility method to get the s3:// path of a bucket. */
  public static String getS3Path(String bucketName, String bucketPrefix) {
    return String.format("s3://%s/%s/", bucketName, bucketPrefix);
  }
}
