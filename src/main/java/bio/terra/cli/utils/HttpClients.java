package bio.terra.cli.utils;

import bio.terra.workspace.client.JSON;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import okhttp3.OkHttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
// Import the connector we use to workaround the JDK17 issues in Jersey
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * Many client libraries for Terra services maintain their own threadpools, but the CLI constantly
 * creates and deletes these clients, leading to a lot of hanging threads (and OutOfMemory
 * exceptions!). To avoid this, we maintain shared client objects
 */
public class HttpClients {
  private static final OkHttpClient okHttpClient;
  private static final Client jaxClient;

  static {
    okHttpClient = new OkHttpClient.Builder().build();
    jaxClient = buildjaxClient();
  }

  private static Client buildjaxClient() {
    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(MultiPartFeature.class);
    clientConfig.register(new JSON());
    clientConfig.register(JacksonFeature.class);
    clientConfig.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
    clientConfig.connectorProvider(new JdkConnectorProvider());
    return ClientBuilder.newClient(clientConfig);
  }

  private HttpClients() {}
  ;

  public static OkHttpClient getOkHttpClient() {
    return okHttpClient;
  }

  public static Client getJaxClient() {
    return jaxClient;
  }
}
