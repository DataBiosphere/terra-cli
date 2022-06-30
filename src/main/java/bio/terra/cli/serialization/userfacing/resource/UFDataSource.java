package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.DataSource;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.UUID;

/**
 * External representation of a data source for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link DataSource} class for internal representation.
 */
@JsonDeserialize(builder = UFDataSource.Builder.class)
public class UFDataSource extends UFResource {
  public final UUID dataSourceWorkspaceUuid;
  // Fields from PF-1703
  // TODO(PF-1724): After PF-1738 is done, add Created On and Last Updated
  public final String title;
  public String shortDescription;
  public String version;

  /** Serialize an instance of the internal class to the command format. */
  public UFDataSource(DataSource internalObj) {
    super(internalObj);
    this.dataSourceWorkspaceUuid = internalObj.getDataSourceWorkspaceUuid();

    Workspace workspace = internalObj.getDataSourceWorkspace();
    this.title = workspace.getName();
    this.shortDescription = workspace.getProperty(DataSource.SHORT_DESCRIPTION_KEY).orElse("");
    this.version = workspace.getProperty(DataSource.VERSION_KEY).orElse("");
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFDataSource(Builder builder) {
    super(builder);
    this.dataSourceWorkspaceUuid = builder.dataSourceWorkspaceUuid;
    this.title = builder.title;
    this.shortDescription = builder.shortDescription;
    this.version = builder.version;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    // Don't call super.print().
    // - We don't want to print reference name; we want data source workspace name.
    // - Printing stewardship doesn't make sense for data sources.
    // - Etc
    // Instead, print fields in PF-1703.
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Title:\t\t\t" + title);
    OUT.println(prefix + "Short description:\t" + shortDescription);
    OUT.println(prefix + "Version:\t\t" + version);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private UUID dataSourceWorkspaceUuid;
    private String title;
    private String shortDescription;
    private String version;

    public Builder dataSourceWorkspaceUuid(UUID dataSourceWorkspaceUuid) {
      this.dataSourceWorkspaceUuid = dataSourceWorkspaceUuid;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder shortDescription(String shortDescription) {
      this.shortDescription = shortDescription;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    /** Call the private constructor. */
    public UFDataSource build() {
      return new UFDataSource(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
