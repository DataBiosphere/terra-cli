package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.utils.PropertiesUtils;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.Folder;
import bio.terra.workspace.model.Properties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.UUID;

@JsonDeserialize(builder = UFFolder.Builder.class)
public class UFFolder {
  public final UUID id;
  public final String displayName;
  public final String description;
  public final UUID parentId;

  public final Properties properties;

  public UFFolder(Folder folder) {
    id = folder.getId();
    displayName = folder.getDisplayName();
    description = folder.getDescription();
    parentId = folder.getParentFolderId();
    properties = folder.getProperties();
  }

  public UFFolder(Builder builder) {
    id = builder.id;
    displayName = builder.displayName;
    description = builder.description;
    parentId = builder.parentId;
    properties = builder.properties;
  }

  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("ID:                " + id);
    OUT.println("Name:              " + displayName);
    OUT.println("Description:       " + description);
    OUT.println("Parent ID:  " + parentId);
    if (properties != null) {
      OUT.println("Properties:");
      PropertiesUtils.propertiesToStringMap(properties)
          .forEach((key, value) -> OUT.println("  " + key + ": " + value));
    }
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private UUID id;
    private String displayName;
    private String description;
    private UUID parentId;

    private Properties properties;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UFFolder.Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public UFFolder.Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public UFFolder.Builder description(String description) {
      this.description = description;
      return this;
    }

    public UFFolder.Builder parentId(UUID parentId) {
      this.parentId = parentId;
      return this;
    }

    public UFFolder.Builder properties(Properties properties) {
      this.properties = properties;
      return this;
    }

    /** Call the private constructor. */
    public UFFolder build() {
      return new UFFolder(this);
    }
  }
}
