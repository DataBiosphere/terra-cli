package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.DataCollection;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * External representation of a data collection for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link DataCollection} class for internal representation.
 */
@JsonDeserialize(builder = UFDataCollection.Builder.class)
public class UFDataCollection extends UFResource {
  private static final Logger logger = LoggerFactory.getLogger(UFDataCollection.class);
  public final UUID dataCollectionWorkspaceUuid;
  // Fields from PF-1703
  // TODO(PF-1724): After PF-1738 is done, add Created On and Last Updated
  public final String title;
  public final String shortDescription;
  public final String version;
  public final List<UFResource> resources;
  public final @Nullable OffsetDateTime createdDate;
  public final @Nullable OffsetDateTime lastUpdatedDate;

  /** Serialize an instance of the internal class to the command format. */
  public UFDataCollection(DataCollection internalObj) {
    super(internalObj);
    this.dataCollectionWorkspaceUuid = internalObj.getDataCollectionWorkspaceUuid();

    Workspace workspace = internalObj.getDataCollectionWorkspace();
    this.title = workspace.getName();
    this.shortDescription = workspace.getProperty(DataCollection.SHORT_DESCRIPTION_KEY).orElse("");
    this.version = workspace.getProperty(DataCollection.VERSION_KEY).orElse("");
    this.resources =
        workspace.getResources().stream()
            .map(Resource::serializeToCommand)
            .collect(Collectors.toList());
    this.createdDate = workspace.getCreatedDate();
    this.lastUpdatedDate = workspace.getLastUpdatedDate();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFDataCollection(Builder builder) {
    super(builder);
    this.dataCollectionWorkspaceUuid = builder.dataCollectionWorkspaceUuid;
    this.title = builder.title;
    this.shortDescription = builder.shortDescription;
    this.version = builder.version;
    this.resources = builder.resources;
    this.createdDate = builder.createdDate;
    this.lastUpdatedDate = builder.lastUpdatedDate;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    // Don't call super.print().
    // - We don't want to print reference name; we want data collection workspace name.
    // - Printing stewardship doesn't make sense for data collections.
    // - Etc
    // Instead, print fields in PF-1703.
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Title:\t\t\t" + title);
    OUT.println(prefix + "Short description:\t" + shortDescription);
    OUT.println(prefix + "Version:\t\t" + version);
    printDate(prefix, "Created", createdDate);
    printDate(prefix, "Last updated", lastUpdatedDate);

    OUT.println(prefix + "Resources:");
    TablePrinter<UFResource> printer = UFResourceColumns::values;
    OUT.println(printer.print(resources));
  }

  /** Column information for data collection resources `terra resource describe` output */
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
    private UUID dataCollectionWorkspaceUuid;
    private String title;
    private String shortDescription;
    private String version;
    private List<UFResource> resources;
    private OffsetDateTime createdDate;
    private OffsetDateTime lastUpdatedDate;

    public Builder dataCollectionWorkspaceUuid(UUID dataCollectionWorkspaceUuid) {
      this.dataCollectionWorkspaceUuid = dataCollectionWorkspaceUuid;
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

    public Builder createdDate(OffsetDateTime createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Builder lastUpdatedDate(OffsetDateTime lastUpdatedDate) {
      this.lastUpdatedDate = lastUpdatedDate;
      return this;
    }

    /** Call the private constructor. */
    public UFDataCollection build() {
      return new UFDataCollection(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  private void printDate(String prefix, String dateLabel, @Nullable OffsetDateTime dateTime) {
    if (dateTime == null) {
      logger.info(String.format("datetime for %s is null, expected for old workspaces", dateLabel));
      return;
    }
    UserIO.getOut()
        .println(prefix + dateLabel + ":\t\t" + dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE));
  }
}
