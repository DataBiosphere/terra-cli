package unit;

import static org.mockito.Mockito.*;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.cli.utils.mount.handlers.GcsFuseMountHandler;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@Tag("real-unit")
public class MountControllerTest {

  @TempDir static Path tempWorkspaceDir;

  @Disabled
  @Test
  @EnabledOnOs(OS.LINUX)
  @DisplayName("mountController mounts buckets on linux")
  void testMountResources() {
    // Arrange
    Workspace workspace = mock(Workspace.class);
    Resource resource1 = mock(Resource.class);
    Resource resource2 = mock(Resource.class);
    BaseMountHandler mountHandler1 = mock(GcsFuseMountHandler.class);
    BaseMountHandler mountHandler2 = mock(GcsFuseMountHandler.class);

    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();

    Map<UUID, Path> resourceMountPaths = new HashMap<>();
    resourceMountPaths.put(uuid1, Path.of("/tmp/mount1"));
    resourceMountPaths.put(uuid2, Path.of("/tmp/mount2"));

    when(workspace.getResource(eq(uuid1))).thenReturn(resource1);
    when(workspace.getResource(eq(uuid2))).thenReturn(resource2);

    doNothing().when(mountHandler1).mount();
    doNothing().when(mountHandler2).mount();

    when(MountController.getMountHandler(eq(resource1), any(Path.class), anyBoolean()))
        .thenReturn(mountHandler1);
    when(MountController.getMountHandler(eq(resource2), any(Path.class), anyBoolean()))
        .thenReturn(mountHandler2);

    // Act
    MountController mountController = MountControllerFactory.getMountController();
    mountController.mountResources(workspace, false);

    // Assert
    verify(workspace, times(1)).getResource(eq(uuid1));
    verify(workspace, times(1)).getResource(eq(uuid2));
    verify(mountHandler1, times(1)).mount();
    verify(mountHandler2, times(1)).mount();
  }

  @Disabled
  @Test
  @EnabledOnOs(OS.LINUX)
  @DisplayName("mountController unmounts buckets on linux")
  void testUnmountResources() {}
}
