package bio.terra.cli.service.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.workspace.model.AwsCredential;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

/** Utilities for AWS resources. */
public class AwsUtils {
  /**
   * Create a console URL for a given destination URL
   *
   * @param awsCredential {@link AwsCredential}
   * @param duration duration of access in seconds
   * @param destinationUrl destination of the console URL
   * @return console URL
   * @throws SystemException Error in URL creation
   */
  public static URL createConsoleUrl(AwsCredential awsCredential, int duration, URL destinationUrl)
      throws SystemException {
    JSONObject credentialObject = new JSONObject();
    credentialObject.put("sessionId", awsCredential.getAccessKeyId());
    credentialObject.put("sessionKey", awsCredential.getSecretAccessKey());
    credentialObject.put("sessionToken", awsCredential.getSessionToken());

    try {
      URLConnection urlConnection =
          new URIBuilder()
              .setScheme("https")
              .setHost("signin.aws.amazon.com")
              .setPath("federation")
              .setParameter("Action", "getSigninToken")
              .setParameter("DurationSeconds", String.valueOf(duration))
              .setParameter("SessionType", "json")
              .setParameter("Session", credentialObject.toString())
              .build()
              .toURL()
              .openConnection();

      BufferedReader bufferReader =
          new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      String urlSigninToken = new JSONObject(bufferReader.readLine()).getString("SigninToken");
      bufferReader.close();

      return new URIBuilder()
          .setScheme("https")
          .setHost("signin.aws.amazon.com")
          .setPath("federation")
          .setParameter("Action", "login")
          .setParameter("Issuer", "terra.verily.com")
          .setParameter("Destination", destinationUrl.toString())
          .setParameter("SigninToken", urlSigninToken)
          .build()
          .toURL();

    } catch (URISyntaxException | IOException e) {
      throw new SystemException("Failed to create destination URL.", e);
    }
  }
}
