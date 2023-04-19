package unit;

import static org.mockito.Mockito.*;

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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

@Tag("real-unit")
public class MountHandlerTest {

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

  @TestFactory
  public Collection<DynamicTest> mountGcsBucketTests() {

    List<DynamicTest> tests = new ArrayList<>();

    for (List<String> bucketMountTest : BUCKET_MOUNT_TESTS) {
      String testName = bucketMountTest.get(0);
      String bucketName = bucketMountTest.get(1);
      String expectedMountPathSuffix = bucketMountTest.get(2);
      String errorMessage = bucketMountTest.get(3);

      tests.add(
          DynamicTest.dynamicTest(
              testName,
              () -> {
                try (MockedStatic<LocalProcessLauncher> localProcessLauncherMockedStatic =
                    mockStatic(LocalProcessLauncher.class)) {
                  // Create test gcs bucket
                  Resource gcsBucket = createTestGcsBucket(bucketName);
                  Path mountPath = tempWorkspaceDir.resolve(Paths.get(bucketName));
                  MountController.createResourceDirectories(List.of(mountPath));

                  // mock the behavior of the LocalProcessLauncher
                  LocalProcessLauncher launcherMock = mock(LocalProcessLauncher.class);
                  int exitValue = errorMessage.isEmpty() ? 0 : 1;
                  when(launcherMock.waitForTerminate()).thenReturn(exitValue);
                  when(launcherMock.getErrorString()).thenReturn(errorMessage);

                  localProcessLauncherMockedStatic
                      .when(LocalProcessLauncher::createLocalProcessLauncher)
                      .thenReturn(launcherMock);

                  // Run mount handler
                  BaseMountHandler mountHandler =
                      MountController.getMountHandler(gcsBucket, mountPath, false);
                  mountHandler.mount();

                  // Check that the mount path does not exist
                  assert Files.exists(
                      mountPath.resolveSibling(mountPath + expectedMountPathSuffix));
                }
              }));
    }

    return tests;
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
