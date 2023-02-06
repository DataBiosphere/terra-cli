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

  static {
    samClient = new ApiClient().getHttpClient();
    wsmClient = new bio.terra.workspace.client.ApiClient().getHttpClient();
  }

  private HttpClients() {}

  public static OkHttpClient getSamClient() {
    return samClient;
  }

  public static Client getWsmClient() {
    return wsmClient;
  }
}
