package bio.terra.cli.utils;

import bio.terra.workspace.client.JSON;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jdk.connector.JdkConnectorProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

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
    wsmClient = buildWsmClient();
    dataRepoClient = new bio.terra.datarepo.client.ApiClient().getHttpClient();
    userManagerClient = new bio.terra.user.client.ApiClient().getHttpClient();
  }

  private static Client buildWsmClient() {
    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(MultiPartFeature.class);
    clientConfig.register(new JSON());
    clientConfig.register(JacksonFeature.class);
    clientConfig.connectorProvider(new JdkConnectorProvider());
    clientConfig.property(JdkConnectorProperties.CONTAINER_IDLE_TIMEOUT, 120000);
    return ClientBuilder.newClient(clientConfig);
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
