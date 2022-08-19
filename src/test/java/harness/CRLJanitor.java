package harness;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.common.JanitorException;
import bio.terra.cloudres.common.cleanup.CleanupConfig;
import bio.terra.janitor.model.CloudResourceUid;
import bio.terra.janitor.model.CreateResourceRequestBody;
import bio.terra.janitor.model.GoogleProjectUid;
import bio.terra.janitor.model.ResourceMetadata;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * This class holds pointers to the hard-coded configuration for CRL Janitor that tests can use to
 * create external resources that will get automatically cleaned up if e.g. the tests fail.
 */
public class CRLJanitor {
  // Map from CLI server to Janitor's identifier for WSM instance.
  public static final Map<String, String> serverToWsmInstanceIdentifier =
      ImmutableMap.of("broad-dev", "dev");
  // CRL janitor client SA
  private static final String SA_KEY_FILE =
      "./rendered/" + TestConfig.getTestConfigName() + "/janitor-client.json";
  // default scope to request for the SA
  private static final List<String> CLOUD_PLATFORM_SCOPE =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
  private static final String DEFAULT_CLIENT_NAME = "cli-test";
  // How long Janitor should wait before cleaning test workspaces. It might be useful
  // to keep these workspaces around long enough to debug, this should be lowered if not.
  private static final Duration WORKSPACE_TIME_TO_LIVE = Duration.ofHours(120);
  // Publisher objects are heavyweight and we use the same credentials for all publishing, so
  // it's better to re-use a single Publisher instance.
  private static final Publisher publisher = initializeJanitorPubSubPublisher();

  public static final ClientConfig getClientConfig() {
    ClientConfig.Builder builder = ClientConfig.Builder.newBuilder().setClient(DEFAULT_CLIENT_NAME);
    if (TestConfig.get().useJanitor()) {
      builder.setCleanupConfig(
          CleanupConfig.builder()
              .setTimeToLive(Duration.ofHours(2))
              .setCleanupId("cli-test-" + System.getProperty("TEST_RUN_ID"))
              .setCredentials(CRLJanitor.getSACredentials())
              .setJanitorTopicName(TestConfig.get().getJanitorPubSubTopic())
              .setJanitorProjectId(TestConfig.get().getJanitorPubSubProjectId())
              .build());
    }
    return builder.build();
  }

  /** Get credentials for the Janitor client SA. */
  private static GoogleCredentials getSACredentials() {
    try {
      return ServiceAccountCredentials.fromStream(new FileInputStream(SA_KEY_FILE))
          .createScoped(CLOUD_PLATFORM_SCOPE);
    } catch (IOException ioEx) {
      throw new RuntimeException("Error reading SA credentials for Janitor client.", ioEx);
    }
  }

  private static Publisher initializeJanitorPubSubPublisher() {
    if (!TestConfig.get().useJanitor()) {
      return null;
    }
    TopicName topicName =
        TopicName.of(
            TestConfig.get().getJanitorPubSubProjectId(), TestConfig.get().getJanitorPubSubTopic());
    try {
      return Publisher.newBuilder(topicName)
          .setCredentialsProvider(FixedCredentialsProvider.create(getSACredentials()))
          .build();
    } catch (IOException e) {
      throw new JanitorException("Failed to initialize Janitor pubsub publisher.", e);
    }
  }

  /**
   * If Janitor is enabled, register the given workspace to be cleaned up. Otherwise return
   * immediately.
   */
  public static void registerWorkspaceForCleanup(UUID uuid, TestUser testUser) {
    if (!TestConfig.get().useJanitor()) {
      return;
    }
    String wsmInstance = Context.getServer().getWorkspaceManagerUri();
    OffsetDateTime curOffsetDateTime = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    CreateResourceRequestBody janitorRequest =
        new CreateResourceRequestBody()
            .resourceUid(
                new CloudResourceUid()
                    .googleProjectUid(
                        new GoogleProjectUid().projectId(UUID.randomUUID().toString())))
            .resourceMetadata(new ResourceMetadata().googleProjectParent("folder/1234"))
            .creation(curOffsetDateTime)
            .expiration(curOffsetDateTime.plus(WORKSPACE_TIME_TO_LIVE));
    ByteString data;
    try {
      data = ByteString.copyFromUtf8(JacksonMapper.getMapper().writeValueAsString(janitorRequest));
    } catch (IOException e) {
      throw new JanitorException(
          String.format("Failed to serialize CreateResourceRequestBody: [%s]", janitorRequest), e);
    }
    PubsubMessage janitorMessage = PubsubMessage.newBuilder().setData(data).build();
    ApiFuture<String> messageIdFuture = publisher.publish(janitorMessage);
    try {
      // Wait for the Future to complete
      messageIdFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new JanitorException(
          String.format("Failed to publish message: [%s] ", data.toString()), e);
    }
  }
}
