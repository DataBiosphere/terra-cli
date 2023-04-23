package unit;

import static org.mockito.Mockito.*;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.cli.utils.mount.handlers.GcsFuseMountHandler;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

@Tag("real-unit")
public class MountControllerTest {

  @TempDir static Path tempWorkspaceDir;
  @Mock private static MockedStatic<LocalProcessLauncher> mockStaticLocalProcessLauncher;

  @BeforeAll
  public static void setup() {
    mockStaticLocalProcessLauncher = mockStatic(LocalProcessLauncher.class);
  }

  @AfterAll
  public static void tearDown() {
    mockStaticLocalProcessLauncher.close();
  }

  /** Utility method to check if a directory exists */
  public void verifyDirectory(Path path) {
    File file = path.toFile();
    assert (file.exists() && file.isDirectory());
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @DisplayName("mountController mounts buckets on linux")
  void testMountResources() {
    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();
    Map<UUID, Path> resourceMountPaths =
        Map.of(
            uuid1, tempWorkspaceDir.resolve(Path.of("bucket-1")),
            uuid2, tempWorkspaceDir.resolve(Path.of("bucket-2")));

    Workspace workspace = mock(Workspace.class);
    Resource resource1 = mock(Resource.class);
    Resource resource2 = mock(Resource.class);
    when(resource1.getResourceType()).thenReturn(Resource.Type.GCS_BUCKET);
    when(resource2.getResourceType()).thenReturn(Resource.Type.GCS_BUCKET);
    when(workspace.getResource(any(UUID.class))).thenReturn(resource1);

    BaseMountHandler mountHandler1 = mock(GcsFuseMountHandler.class);
    BaseMountHandler mountHandler2 = mock(GcsFuseMountHandler.class);
    doNothing().when(mountHandler1).mount();
    doNothing().when(mountHandler2).mount();

    MountController mountController = MountControllerFactory.getMountController();
    MountController spyMountController = spy(mountController);
    Mockito.doReturn(resourceMountPaths).when(spyMountController).getResourceMountPaths();

    Mockito.doReturn(mountHandler1)
        .when(spyMountController)
        .getMountHandler(any(Resource.class), eq(resourceMountPaths.get(uuid1)), anyBoolean());
    Mockito.doReturn(mountHandler2)
        .when(spyMountController)
        .getMountHandler(any(Resource.class), eq(resourceMountPaths.get(uuid2)), anyBoolean());

    spyMountController.mountResources(workspace, false);

    verify(workspace, times(1)).getResource(eq(uuid1));
    verify(workspace, times(1)).getResource(eq(uuid2));
    verify(mountHandler1, times(1)).mount();
    verify(mountHandler2, times(1)).mount();

    verifyDirectory(resourceMountPaths.get(uuid1));
    verifyDirectory(resourceMountPaths.get(uuid2));
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @DisplayName("mountController unmounts buckets on linux")
  void testUnmountResources() {
    System.setProperty("os.name", "Linux");

    // Example output of `mount` command on ubuntu
    InputStream mountOutput =
        new ByteArrayInputStream(
            ("mqueue on /dev/mqueue type mqueue (rw,relatime)\n"
                    + "/dev/sda15 on /boot/efi type vfat (rw,relatime,fmask=0022,dmask=0022,codepage=437,iocharset=ascii,shortname=mixed,utf8,errors=remount-ro)\n"
                    + "fusectl on /sys/fs/fuse/connections type fusectl (rw,relatime)\n"
                    + "bucket-1 on "
                    + tempWorkspaceDir
                    + "/bucket-1 type fuse.gcsfuse (rw,nosuid,nodev,relatime,user_id=1000,group_id=1001,default_permissions)\n"
                    + "bucket-2 on "
                    + tempWorkspaceDir
                    + "/bucket-2 type fuse.gcsfuse (rw,nosuid,nodev,relatime,user_id=1000,group_id=1001,default_permissions)\n"
                    + "bucket-3 on "
                    + tempWorkspaceDir
                    + "/bucket-3 type fuse.gcsfuse (rw,nosuid,nodev,relatime,user_id=1000,group_id=1001,default_permissions)\n")
                .getBytes());

    MockedStatic<MountController> mockStaticMountController = mockStatic(MountController.class);
    mockStaticMountController.when(MountController::getWorkspaceDir).thenReturn(tempWorkspaceDir);

    LocalProcessLauncher launcherMock = mock(LocalProcessLauncher.class);
    when(launcherMock.waitForTerminate()).thenReturn(0);
    when(launcherMock.getInputStream()).thenReturn(mountOutput);
    mockStaticLocalProcessLauncher.when(LocalProcessLauncher::create).thenReturn(launcherMock);

    MockedStatic<BaseMountHandler> mockStaticBaseMountHandler = mockStatic(BaseMountHandler.class);
    mockStaticBaseMountHandler
        .when(() -> BaseMountHandler.unmount(any(String.class)))
        .then((Answer<Void>) invocation -> null);

    // Run unmountResources
    MountController mountController = MountControllerFactory.getMountController();
    mountController.unmountResources();

    // Verify that BaseMountHandler.unmount has been called for each bucket
    mockStaticBaseMountHandler.verify(() -> BaseMountHandler.unmount("bucket-1"));
    mockStaticBaseMountHandler.verify(() -> BaseMountHandler.unmount("bucket-2"));
    mockStaticBaseMountHandler.verify(() -> BaseMountHandler.unmount("bucket-3"));

    mockStaticMountController.close();
    mockStaticBaseMountHandler.close();
  }
}
