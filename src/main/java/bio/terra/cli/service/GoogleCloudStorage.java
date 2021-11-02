package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import com.google.auth.oauth2.GoogleCredentials;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for talking to Google Cloud Storage. */
public class GoogleCloudStorage {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

  private final StorageCow storage;

  /**
   * Factory method for class that talks to GCS. Pulls the current user from the context. Uses the
   * pet SA credentials instead of the end user credentials because we need the cloud-platform scope
   * to talk to the cloud directly. The CLI does not request the cloud-platform scope during the
   * user login flow, so we need to use the pet SA credentials instead when that scope is needed.
   */
  public static GoogleCloudStorage fromContextForPetSa() {
    return new GoogleCloudStorage(Context.requireUser().getPetSACredentials());
  }

  private GoogleCloudStorage(GoogleCredentials credentials) {
    storage = CrlUtils.createStorageCow(credentials);
  }

  public Optional<BucketCow> getBucket(String bucketName) {
    try {
      return Optional.of(storage.get(bucketName));
    } catch (Exception ex) {
      logger.error("Caught exception looking up bucket", ex);
      return Optional.empty();
    }
  }

  public long getNumObjects(BucketCow bucket) {
    int numObjectsCtr = 0;
    while (bucket.list().getValues().iterator().hasNext()) {
      numObjectsCtr++;
    }
    return numObjectsCtr;
  }
}
