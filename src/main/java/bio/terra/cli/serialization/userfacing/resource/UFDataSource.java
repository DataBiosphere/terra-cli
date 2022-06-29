package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.DataSource;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.UUID;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  public final List<UFResource> resources;

  /** Serialize an instance of the internal class to the command format. */
  public UFDataSource(DataSource internalObj) {
    super(internalObj);
    this.dataSourceWorkspaceUuid = internalObj.getDataSourceWorkspaceUuid();

    Workspace workspace = internalObj.getDataSourceWorkspace();
    this.title = workspace.getName();
    this.shortDescription = workspace.getProperty(DataSource.SHORT_DESCRIPTION_KEY).orElse("");
    this.version = workspace.getProperty(DataSource.VERSION_KEY).orElse("");
    this.resources =
        workspace.getResources().stream()
            .map(Resource::serializeToCommand)
            .collect(Collectors.toList());
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFDataSource(Builder builder) {
    super(builder);
    this.dataSourceWorkspaceUuid = builder.dataSourceWorkspaceUuid;
    this.title = builder.title;
    this.shortDescription = builder.shortDescription;
    this.version = builder.version;
    this.resources = builder.resources;
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

    OUT.println(prefix + "Resources:");
    TablePrinter<UFResource> printer = UFResourceColumns::values;
    OUT.println(printer.print(resources));
  }

  /** Column information for data source resources in data source `resource describe` output */
  private enum UFResourceColumns implements ColumnDefinition<UFResource> {
    BLANK_COLUMN("", r -> " ", 22, Alignment.LEFT),
    NAME("NAME", r -> r.name, 30, Alignment.LEFT),
    RESOURCE_TYPE("RESOURCE TYPE", r -> r.resourceType.toString(), 20, Alignment.LEFT),
    DESCRIPTION("DESCRIPTION", r -> r.description, 40, Alignment.LEFT);

    private final String columnLabel;
    private final Function<UFResource, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    UFResourceColumns(
        String columnLabel,
        Function<UFResource, String> valueExtractor,
        int width,
        Alignment alignment) {
      this.columnLabel = columnLabel;
      this.valueExtractor = valueExtractor;
      this.width = width;
      this.alignment = alignment;
    }

    @Override
    public String getLabel() {
      return columnLabel;
    }

    @Override
    public Function<UFResource, String> getValueExtractor() {
      return valueExtractor;
    }

    @Override
    public int getWidth() {
      return width;
    }

    @Override
    public Alignment getAlignment() {
      return alignment;
    }
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private UUID dataSourceWorkspaceUuid;
    private String title;
    private String shortDescription;
    private String version;
    private List<UFResource> resources;

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

    public Builder resources(List<UFResource> resources) {
      this.resources = resources;
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
