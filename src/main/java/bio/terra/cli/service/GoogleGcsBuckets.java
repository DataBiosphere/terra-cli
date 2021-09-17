package bio.terra.cli.service;

import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.storage.BlobCow;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import java.util.stream.StreamSupport;

public class GoogleGcsBuckets {
  private final StorageCow storageCow;

  public GoogleGcsBuckets(GoogleCredentials credentials) {
    this.storageCow = CrlUtils.createStorageCow(credentials);
  }

  /**
   * Get an exact (slow) count of the objects in a bucket.
   *
   * @param bucketName - name of the bucket
   * @return count
   */
  public long getBucketObjectCount(String bucketName) {
    BucketCow bucketCow = storageCow.get(bucketName);
    Page<BlobCow> page = bucketCow.list();
    long count = countPageItems(page);
    while (page.hasNextPage()) {
      page = page.getNextPage();
      count += countPageItems(page);
    }
    return count;
  }

  private long countPageItems(Page<BlobCow> page) {
    return StreamSupport.stream(page.iterateAll().spliterator(), false).count();
  }
}
