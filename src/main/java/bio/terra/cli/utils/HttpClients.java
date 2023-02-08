package bio.terra.cli.utils;

import javax.ws.rs.client.Client;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;

/**
 * Many client libraries for Terra services maintain their own thread pools, but the CLI constantly
 * creates and deletes these clients, leading to a lot of hanging threads and memory problems
 * (especially during tests). To avoid this, we maintain shared client objects
 */
public class HttpClients {
  private static final OkHttpClient samClient;
  private static final Client wsmClient;
  private static final Client dataRepoClient;
  private static final Client userManagerClient;

  static {
    samClient = new ApiClient().getHttpClient();
    wsmClient = new bio.terra.workspace.client.ApiClient().getHttpClient();
    dataRepoClient = new bio.terra.datarepo.client.ApiClient().getHttpClient();
    userManagerClient = new bio.terra.user.client.ApiClient().getHttpClient();
  }

  private HttpClients() {}

  public static OkHttpClient getSamClient() {
    return samClient;
  }

  public static Client getWsmClient() {
    return wsmClient;
  }

  public static Client getDataRepoClient() {
    return dataRepoClient;
  }

  public static Client getUserManagerClient() {
    return userManagerClient;
  }
}
