package unit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.OSFamily;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.cli.utils.mount.handlers.GcsFuseMountHandler;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

@Tag("unit")
public class MountControllerTest {

  @TempDir static Path tempWorkspaceDir;

  /** Utility method to check if a directory exists */
  public void verifyDirectory(Path path) {
    File file = path.toFile();
    assert (file.exists() && file.isDirectory());
  }

  @Test
  @DisplayName("mountController mounts buckets on linux")
  void testMountResources() {
    try (MockedStatic<Context> mockStaticContext = mockStatic(Context.class)) {

      Path mountPath1 = tempWorkspaceDir.resolve(Path.of("bucket-1"));
      Path mountPath2 = tempWorkspaceDir.resolve(Path.of("bucket-2"));

      Resource resource1 = mock(Resource.class);
      Resource resource2 = mock(Resource.class);
      when(resource1.getResourceType()).thenReturn(Resource.Type.GCS_BUCKET);
      when(resource2.getResourceType()).thenReturn(Resource.Type.GCS_BUCKET);

      Workspace workspace = mock(Workspace.class);
      when(workspace.listResources()).thenReturn(List.of(resource1, resource2));

      mockStaticContext.when(Context::requireWorkspace).thenReturn(workspace);

      BaseMountHandler mountHandler1 = mock(GcsFuseMountHandler.class);
      BaseMountHandler mountHandler2 = mock(GcsFuseMountHandler.class);
      when(mountHandler1.mount()).thenReturn(0);
      when(mountHandler2.mount()).thenReturn(0);

      MountController mountController = MountControllerFactory.getMountController();
      MountController spyMountController = spy(mountController);

      doReturn(new HashMap<UUID, Path>()).when(spyMountController).getFolderIdToFolderPathMap();

      doReturn(mountPath1, mountPath2)
          .when(spyMountController)
          .getResourceMountPath(any(Resource.class), anyMap());

      doReturn(mountHandler1, mountHandler2)
          .when(spyMountController)
          .getMountHandler(any(Resource.class), eq(mountPath1), anyBoolean(), anyBoolean());
      doReturn(mountHandler2)
          .when(spyMountController)
          .getMountHandler(any(Resource.class), eq(mountPath2), anyBoolean(), anyBoolean());

      // Call mount resources
      spyMountController.mountResources(false, true);

      // Validate that handlers are called and directories are created
      verify(mountHandler1, times(1)).mount();
      verify(mountHandler2, times(1)).mount();

      verifyDirectory(mountPath1);
      verifyDirectory(mountPath2);
    }
  }

  @Test
  @DisplayName("mountController unmounts buckets on linux")
  void testUnmountResources() {
    // Example output of `mount` command on ubuntu
    InputStream linuxMountOutput =
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
    InputStream macMountOutput =
        new ByteArrayInputStream(
            ("/dev/disk3s1s1 on / (apfs, sealed, local, read-only, journaled)\n"
                    + "devfs on /dev (devfs, local, nobrowse)\n"
                    + "/dev/disk3s6 on /System/Volumes/VM (apfs, local, noexec, journaled, noatime, nobrowse)\n"
                    + "srcfsd_darwin@googleosxfuse0 on /Volumes/google/src (googleosxfuse, nodev, nosuid, synchronous, mounted by rogerwangcs)\n"
                    + "bucket-1 on "
                    + tempWorkspaceDir
                    + "/bucket-1 (macfuse, nodev, nosuid, synchronous, mounted by me)\n"
                    + "bucket-2 on "
                    + tempWorkspaceDir
                    + "/bucket-2 (macfuse, nodev, nosuid, synchronous, mounted by me)\n"
                    + "bucket-3 on "
                    + tempWorkspaceDir
                    + "/bucket-3 (macfuse, nodev, nosuid, synchronous, mounted by me)\n")
                .getBytes());
    InputStream mountOutput =
        OSFamily.getOSFamily().equals(OSFamily.LINUX) ? linuxMountOutput : macMountOutput;

    try (MockedStatic<LocalProcessLauncher> mockStaticLocalProcessLauncher =
            mockStatic(LocalProcessLauncher.class);
        MockedStatic<MountController> mockStaticMountController =
            mockStatic(MountController.class);
        MockedStatic<BaseMountHandler> mockStaticBaseMountHandler =
            mockStatic(BaseMountHandler.class)) {
      mockStaticMountController.when(MountController::getWorkspaceDir).thenReturn(tempWorkspaceDir);

      LocalProcessLauncher launcherMock = mock(LocalProcessLauncher.class);
      when(launcherMock.waitForTerminate()).thenReturn(0);
      when(launcherMock.getInputStream()).thenReturn(mountOutput);
      mockStaticLocalProcessLauncher.when(LocalProcessLauncher::create).thenReturn(launcherMock);

      mockStaticBaseMountHandler
          .when(() -> BaseMountHandler.unmount(any(String.class)))
          .then((Answer<Void>) invocation -> null);

      // Run unmountResources
      MountController mountController = MountControllerFactory.getMountController();
      mountController.unmountResources();

      // Verify that BaseMountHandler.unmount has been called for each bucket
      mockStaticBaseMountHandler.verify(
          () -> BaseMountHandler.unmount(tempWorkspaceDir + "/bucket-1"));
      mockStaticBaseMountHandler.verify(
          () -> BaseMountHandler.unmount(tempWorkspaceDir + "/bucket-2"));
      mockStaticBaseMountHandler.verify(
          () -> BaseMountHandler.unmount(tempWorkspaceDir + "/bucket-3"));
    }
  }
}
