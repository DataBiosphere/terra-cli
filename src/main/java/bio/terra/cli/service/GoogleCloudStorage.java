package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.storage.BlobCow;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import java.util.stream.StreamSupport;

/** Wrapper service for the CRL Google Storage COW */
public class GoogleCloudStorage {
  private final StorageCow storageCow;

  public GoogleCloudStorage(GoogleCredentials credentials) {
    this.storageCow = CrlUtils.createStorageCow(credentials);
  }

  /**
   * Create a GoogleCloudStorage instance with the current user's pet storage account credentials
   *
   * @return - GoogleCloudStorage instance
   */
  public static GoogleCloudStorage fromContextForSa() {
    return new GoogleCloudStorage(Context.requireUser().getPetSACredentials());
  }

  /**
   * Get an exact (slow) count of the objects in a bucket up to limit, since counting exact number
   * is potentially slow and expensive.
   *
   * @param bucketName - name of the bucket
   * @param limit - max number to count
   * @return count
   */
  public long getBucketObjectCount(String bucketName, long limit) {
    BucketCow bucketCow = storageCow.get(bucketName);
    Page<BlobCow> page = bucketCow.list();
    long count = countPageItems(page);
    if (limit < count) {
      return limit;
    }
    while (page.hasNextPage()) {
      page = page.getNextPage();
      count += countPageItems(page);
      if (limit < count) {
        return limit;
      }
    }
    return count;
  }

  private long countPageItems(Page<BlobCow> page) {
    return StreamSupport.stream(page.iterateAll().spliterator(), false).count();
  }
}
