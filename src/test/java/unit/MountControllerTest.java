package unit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.ControlledResourceMetadata;
import bio.terra.workspace.model.GcpGcsBucketAttributes;
import bio.terra.workspace.model.ResourceAttributesUnion;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import bio.terra.workspace.model.ResourceType;
import bio.terra.workspace.model.StewardshipType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

@Tag("real-unit")
public class MountControllerTest {

  @TempDir static Path tempWorkspaceDir;

  private static final List<List<String>> BUCKET_MOUNT_TESTS =
      List.of(
          List.of("mounts gcs bucket successfully", "good-bucket", "", ""),
          List.of(
              "handles mounting inaccessible gcs bucket",
              "inaccessible-bucket",
              "_NO_ACCESS",
              "Error 403: does not have storage.objects.list access to the Google Cloud Storage bucket. Permission 'storage.objects.list' denied on resource (or it may not exist)., forbidden"),
          List.of(
              "handles mounting nonexistent gcs bucket",
              "nonexistent-bucket",
              "_NOT_FOUND",
              "daemonize.Run: readFromProcess: sub-process: mountWithArgs: mountWithConn: fs.NewServer: create file system: SetUpBucket: Error in iterating through objects: storage: bucket doesn't exist"),
          List.of(
              "handles generic failure when mounting gcs bucket",
              "failed-mount-bucket",
              "_MOUNT_FAILED",
              "daemonize.Run: readFromProcess: sub-process: mountWithArgs: failed to open connection - getConnWithRetry: get token source: DefaultTokenSource: google: could not find default credentials."));

  @Test
  @EnabledOnOs(OS.MAC)
  @DisplayName("mountController mounts buckets on mac")
  void testMountOnMac() {

  }

  private GcsBucket createTestGcsBucket(String bucketName) {
    return new GcsBucket(
        new ResourceDescription()
            .metadata(
                new ResourceMetadata()
                    .resourceId(UUID.randomUUID())
                    .name(bucketName)
                    .resourceType(ResourceType.GCS_BUCKET)
                    .stewardshipType(StewardshipType.CONTROLLED)
                    .controlledResourceMetadata(
                        new ControlledResourceMetadata().accessScope(AccessScope.SHARED_ACCESS)))
            .resourceAttributes(
                new ResourceAttributesUnion()
                    .gcpGcsBucket(new GcpGcsBucketAttributes().bucketName(bucketName))));
  }
}
