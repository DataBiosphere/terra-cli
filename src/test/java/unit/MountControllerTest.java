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
import bio.terra.cli.businessobject.User;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.OSFamily;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.cli.utils.mount.handlers.GcsFuseMountHandler;
import bio.terra.workspace.model.StewardshipType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

@Tag("unit")
public class MountControllerTest {

  @TempDir static Path tempWorkspaceDir;

  @Mock private Workspace workspace;
  @Mock private User user;
  @Mock private Resource resource1;
  @Mock private Resource resource2;
  @Mock private BaseMountHandler mountHandler1;
  @Mock private BaseMountHandler mountHandler2;

  private static Path mountPath1;
  private static Path mountPath2;

  private InputStream getMountOutputStream() {
    String linuxMountOutput =
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
            + "/bucket-3 type fuse.gcsfuse (rw,nosuid,nodev,relatime,user_id=1000,group_id=1001,default_permissions)\n");
    String macMountOutput =
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
            + "/bucket-3 (macfuse, nodev, nosuid, synchronous, mounted by me)\n");
    return OSFamily.getOSFamily().equals(OSFamily.LINUX)
        ? new ByteArrayInputStream(linuxMountOutput.getBytes())
        : new ByteArrayInputStream(macMountOutput.getBytes());
  }

  @BeforeEach
  public void setUpTest() {
    mountPath1 = tempWorkspaceDir.resolve(Path.of("bucket-1"));
    mountPath2 = tempWorkspaceDir.resolve(Path.of("bucket-2"));

    resource1 = mock(Resource.class);
    when(resource1.getName()).thenReturn("bucket-1");
    when(resource1.getResourceType()).thenReturn(Resource.Type.GCS_BUCKET);
    when(resource1.getStewardshipType()).thenReturn(StewardshipType.CONTROLLED);
    when(resource1.getCreatedBy()).thenReturn("johny.appleseed@verily.com");

    resource2 = mock(Resource.class);
    when(resource2.getName()).thenReturn("bucket-2");
    when(resource2.getResourceType()).thenReturn(Resource.Type.GCS_BUCKET);
    when(resource2.getStewardshipType()).thenReturn(StewardshipType.CONTROLLED);
    when(resource2.getCreatedBy()).thenReturn("bonny.bananabead@verily.com");

    workspace = mock(Workspace.class);
    when(workspace.listResources()).thenReturn(List.of(resource1, resource2));
    when(workspace.getResource(resource1.getName())).thenReturn(resource1);
    when(workspace.getResource(resource2.getName())).thenReturn(resource2);

    user = mock(User.class);
    when(user.getEmail()).thenReturn("johny.appleseed@verily.com");

    mountHandler1 = mock(GcsFuseMountHandler.class);
    mountHandler2 = mock(GcsFuseMountHandler.class);
    when(mountHandler1.mount()).thenReturn(0);
    when(mountHandler2.mount()).thenReturn(0);
  }

  /** Utility method to check if a directory exists */
  private void verifyDirectory(Path path) {
    File file = path.toFile();
    assert (file.exists() && file.isDirectory());
  }

  private MountController getSpyMountController() {
    MountController mountController = MountControllerFactory.getMountController();
    MountController spyMountController = spy(mountController);

    doReturn(new HashMap<UUID, Path>()).when(spyMountController).getFolderIdToFolderPathMap();

    doReturn(mountPath1, mountPath2)
        .when(spyMountController)
        .getResourceMountPath(any(Resource.class), anyMap());

    doReturn(true).when(spyMountController).isMountableResource(any(Resource.class));

    doReturn(mountHandler1, mountHandler2)
        .when(spyMountController)
        .getMountHandler(any(Resource.class), eq(mountPath1), anyBoolean(), anyBoolean());
    doReturn(mountHandler2)
        .when(spyMountController)
        .getMountHandler(any(Resource.class), eq(mountPath2), anyBoolean(), anyBoolean());
    return spyMountController;
  }

  @Test
  @DisplayName("mountController mounts buckets on linux")
  void mountResources_succeeds() {
    try (MockedStatic<Context> mockStaticContext = mockStatic(Context.class)) {
      mockStaticContext.when(Context::requireWorkspace).thenReturn(workspace);
      mockStaticContext.when(Context::requireUser).thenReturn(user);

      MountController spyMountController = getSpyMountController();

      // Call mount resources
      spyMountController.mountResources(/*disableCache=*/ false, /*readOnly=*/ null);

      // Validate that handlers are called and directories are created
      verify(mountHandler1, times(1)).mount();
      verify(mountHandler2, times(1)).mount();

      verifyDirectory(mountPath1);
      verifyDirectory(mountPath2);
    }
  }

  @Test
  @DisplayName("mountController mounts buckets with expected read/write permissions")
  void mountResources_mountsWithDefaultPermissions() {
    try (MockedStatic<Context> mockStaticContext = mockStatic(Context.class)) {
      mockStaticContext.when(Context::requireWorkspace).thenReturn(workspace);
      mockStaticContext.when(Context::requireUser).thenReturn(user);

      MountController spyMountController = getSpyMountController();
      spyMountController.mountResources(/*disableCache=*/ false, /*readOnly=*/ null);

      // Validate that buckets created by the current user are mounted as read-write and other
      // buckets are mounted as read.
      verify(spyMountController)
          .getMountHandler(eq(resource1), eq(mountPath1), anyBoolean(), eq(false));
      verify(spyMountController)
          .getMountHandler(eq(resource2), eq(mountPath2), anyBoolean(), eq(true));
    }
  }

  @Test
  @DisplayName("mountController mounts all buckets as read-only")
  void mountResources_mountsAllReadOnly() {
    try (MockedStatic<Context> mockStaticContext = mockStatic(Context.class)) {
      mockStaticContext.when(Context::requireWorkspace).thenReturn(workspace);
      mockStaticContext.when(Context::requireUser).thenReturn(user);

      MountController spyMountController = getSpyMountController();
      spyMountController.mountResources(/*disableCache=*/ false, /*readOnly=*/ true);

      // Validate that readOnly flag overrides default mount permissions
      verify(spyMountController)
          .getMountHandler(eq(resource1), eq(mountPath1), anyBoolean(), eq(true));
      verify(spyMountController)
          .getMountHandler(eq(resource2), eq(mountPath2), anyBoolean(), eq(true));
    }
  }

  @Test
  @DisplayName("mountController mounts all buckets as read-write")
  void mountResources_mountsAllReadWrite() {
    try (MockedStatic<Context> mockStaticContext = mockStatic(Context.class)) {
      mockStaticContext.when(Context::requireWorkspace).thenReturn(workspace);
      mockStaticContext.when(Context::requireUser).thenReturn(user);

      MountController spyMountController = getSpyMountController();
      spyMountController.mountResources(/*disableCache=*/ false, /*readOnly=*/ false);

      // Validate that readOnly flag overrides default mount permissions
      verify(spyMountController)
          .getMountHandler(eq(resource1), eq(mountPath1), anyBoolean(), eq(false));
      verify(spyMountController)
          .getMountHandler(eq(resource2), eq(mountPath2), anyBoolean(), eq(false));
    }
  }

  @Test
  @DisplayName("mountController mount a single bucket")
  void mountResource_succeeds() {
    try (MockedStatic<Context> mockStaticContext = mockStatic(Context.class)) {
      mockStaticContext.when(Context::requireWorkspace).thenReturn(workspace);
      mockStaticContext.when(Context::requireUser).thenReturn(user);

      MountController spyMountController = getSpyMountController();
      spyMountController.mountResource(
          resource1.getName(), /*disableCache=*/ false, /*readOnly=*/ null);

      // Validate that the mount handler has been created and mount method has been called
      verify(spyMountController)
          .getMountHandler(eq(resource1), eq(mountPath1), anyBoolean(), anyBoolean());
      verify(mountHandler1, times(1)).mount();
    }
  }

  @Test
  @DisplayName("mountController unmounts buckets on linux")
  void unmountResources_succeeds() {
    try (MockedStatic<LocalProcessLauncher> mockStaticLocalProcessLauncher =
            mockStatic(LocalProcessLauncher.class);
        MockedStatic<MountController> mockStaticMountController =
            mockStatic(MountController.class);
        MockedStatic<BaseMountHandler> mockStaticBaseMountHandler =
            mockStatic(BaseMountHandler.class)) {
      mockStaticMountController.when(MountController::getWorkspaceDir).thenReturn(tempWorkspaceDir);

      LocalProcessLauncher launcherMock = mock(LocalProcessLauncher.class);
      when(launcherMock.waitForTerminate()).thenReturn(0);
      when(launcherMock.getInputStream()).thenReturn(getMountOutputStream());
      mockStaticLocalProcessLauncher.when(LocalProcessLauncher::create).thenReturn(launcherMock);

      mockStaticBaseMountHandler
          .when(() -> BaseMountHandler.unmount(any(Path.class)))
          .then((Answer<Void>) invocation -> null);

      // Run unmountResources
      MountController mountController = MountControllerFactory.getMountController();
      mountController.unmountResources();

      // Verify that BaseMountHandler.unmount has been called for each bucket
      mockStaticBaseMountHandler.verify(
          () -> BaseMountHandler.unmount(Path.of(tempWorkspaceDir + "/bucket-1")));
      mockStaticBaseMountHandler.verify(
          () -> BaseMountHandler.unmount(Path.of(tempWorkspaceDir + "/bucket-2")));
      mockStaticBaseMountHandler.verify(
          () -> BaseMountHandler.unmount(Path.of(tempWorkspaceDir + "/bucket-3")));
    }
  }

  @Test
  @DisplayName("mountController unmounts a single bucket")
  void unmountResource_succeeds() {
    try (MockedStatic<Context> mockStaticContext = mockStatic(Context.class);
        MockedStatic<LocalProcessLauncher> mockStaticLocalProcessLauncher =
            mockStatic(LocalProcessLauncher.class);
        MockedStatic<MountController> mockStaticMountController =
            mockStatic(MountController.class);
        MockedStatic<BaseMountHandler> mockStaticBaseMountHandler =
            mockStatic(BaseMountHandler.class)) {

      mockStaticContext.when(Context::requireWorkspace).thenReturn(workspace);

      mockStaticMountController.when(MountController::getWorkspaceDir).thenReturn(tempWorkspaceDir);

      LocalProcessLauncher launcherMock = mock(LocalProcessLauncher.class);
      when(launcherMock.waitForTerminate()).thenReturn(0);
      when(launcherMock.getInputStream()).thenReturn(getMountOutputStream());
      mockStaticLocalProcessLauncher.when(LocalProcessLauncher::create).thenReturn(launcherMock);

      mockStaticBaseMountHandler
          .when(() -> BaseMountHandler.unmount(any(Path.class)))
          .then((Answer<Void>) invocation -> null);

      // Run unmountResource
      MountController spyMountController = getSpyMountController();
      doReturn(resource1.getName())
          .when(spyMountController)
          .getBucketNameFromResource(any(Resource.class));
      spyMountController.unmountResource(resource1.getName());

      // Verify that BaseMountHandler.unmount has been called on the resource
      mockStaticBaseMountHandler.verify(
          () -> BaseMountHandler.unmount(Path.of(tempWorkspaceDir + "/bucket-1")));
    }
  }
}
