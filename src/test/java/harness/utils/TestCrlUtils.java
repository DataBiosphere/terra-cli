package harness.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.google.dataproc.DataprocCow;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;

/** Utilities for making gcp cloud calls with CRL in tests */
public class TestCrlUtils {

  private static final ClientConfig clientConfig =
      ClientConfig.Builder.newBuilder().setClient("terra-cli").build();

  public static DataprocCow createDataprocCow(GoogleCredentials googleCredentials) {
    try {
      return DataprocCow.create(clientConfig, googleCredentials);
    } catch (GeneralSecurityException | IOException e) {
      throw new SystemException("Error creating dataproc client.", e);
    }
  }
}
