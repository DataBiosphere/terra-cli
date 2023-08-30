package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.UFFolder;
import bio.terra.cli.utils.PropertiesUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class Folder extends SingleWorkspaceUnit {

  @Test
  public void createFolder_succeeds() throws JsonProcessingException {
    var folderName = "foo";
    var description = "this is a test folder";

    UFFolder f =
        TestCommand.runAndParseCommandExpectSuccess(
            UFFolder.class,
            "terra",
            "folder",
            "create",
            "--name=" + folderName,
            "--description=" + description,
            "--properties=hello=world,apple=red");

    assertEquals(folderName, f.displayName);
    assertEquals(description, f.description);
    assertNull(f.parentId);
    Map<String, String> properties = PropertiesUtils.propertiesToStringMap(f.properties);
    assertEquals("world", properties.get("hello"));
    assertEquals("red", properties.get("apple"));

    // clean up
    TestCommand.runCommandExpectSuccess("terra", "folder", "delete", "--id=" + f.id);
  }

  @Test
  public void createSubFolder_succeeds() throws JsonProcessingException {
    UFFolder folder =
        TestCommand.runAndParseCommandExpectSuccess(
            UFFolder.class,
            "terra",
            "folder",
            "create",
            "--name=foo",
            "--description=this is a test folder",
            "--properties=hello=world,apple=red");

    var subFolderName = "foo2";
    UFFolder subFolder =
        TestCommand.runAndParseCommandExpectSuccess(
            UFFolder.class,
            "terra",
            "folder",
            "create",
            "--name=" + subFolderName,
            "--parent-folder-id=" + folder.id);

    assertEquals(subFolderName, subFolder.displayName);
    assertEquals(folder.id, subFolder.parentId);
    assertNull(subFolder.description);
    assertNull(subFolder.properties);

    // clean up
    TestCommand.runCommandExpectSuccess("terra", "folder", "delete", "--id=" + folder.id);
  }

  @Test
  public void updateFolder_succeeds() throws JsonProcessingException {
    UFFolder folder =
        TestCommand.runAndParseCommandExpectSuccess(
            UFFolder.class,
            "terra",
            "folder",
            "create",
            "--name=foo",
            "--description=this is a test folder",
            "--properties=hello=world,apple=red");
    UFFolder subFolder =
        TestCommand.runAndParseCommandExpectSuccess(
            UFFolder.class,
            "terra",
            "folder",
            "create",
            "--name=bar",
            "--parent-folder-id=" + folder.id);
    assertEquals(folder.id, subFolder.parentId);

    var newName = "bar2";
    var newDescription = "this is folder bar 2";

    UFFolder updatedFolder =
        TestCommand.runAndParseCommandExpectSuccess(
            UFFolder.class,
            "terra",
            "folder",
            "update",
            "--move-to-root",
            "--id=" + subFolder.id,
            "--new-name=" + newName,
            "--new-description=" + newDescription);

    assertNull(updatedFolder.parentId);
    assertEquals(newName, updatedFolder.displayName);
    assertEquals(newDescription, updatedFolder.description);
    // clean up
    TestCommand.runCommandExpectSuccess("terra", "folder", "delete", "--id=" + folder.id);
    TestCommand.runCommandExpectSuccess("terra", "folder", "delete", "--id=" + updatedFolder.id);
  }
}
