package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.GoogleBigQuery;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.services.bigquery.model.Table;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * External representation of a workspace BigQuery data table resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link BqTable} class for a dataset's internal representation.
 */
@JsonDeserialize(builder = UFBqTable.Builder.class)
public class UFBqTable extends UFResource {
  public final String projectId;
  public final String datasetId;
  public final String dataTableId;
  public @Nullable final String tableDescription;
  public final BigInteger numRows;

  /** Serialize an instance of the internal class to the command format. */
  public UFBqTable(BqTable internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();
    this.dataTableId = internalObj.getDataTableId();

    GoogleBigQuery bigQuery = GoogleBigQuery.fromContextForPetSa();
    Optional<Table> dataTableOptional = bigQuery.getDataTable(projectId, datasetId, dataTableId);
    tableDescription = dataTableOptional.map(Table::getDescription).orElse(null);
    numRows = dataTableOptional.map(Table::getNumRows).orElse(null);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFBqTable(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.dataTableId = builder.dataTableId;
    this.tableDescription = builder.tableDescription;
    this.numRows = builder.numRows;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "GCP project id: " + projectId);
    OUT.println(prefix + "BigQuery dataset id: " + datasetId);
    OUT.println(prefix + "BigQuery table id: " + dataTableId);

    if (tableDescription != null) {
      OUT.println(prefix + "Table description: " + tableDescription);
    }
    OUT.println(prefix + "# Rows: " + (numRows == null ? "(unknown)" : numRows.toString()));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String projectId;
    private String datasetId;
    private String dataTableId;
    private @Nullable String tableDescription;
    private BigInteger numRows;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder dataTableId(String dataTableId) {
      this.dataTableId = dataTableId;
      return this;
    }

    public Builder tableDescription(@Nullable String tableDescription) {
      this.tableDescription = tableDescription;
      return this;
    }

    public Builder numRows(BigInteger numRows) {
      this.numRows = numRows;
      return this;
    }
    /** Call the private constructor. */
    public UFBqTable build() {
      return new UFBqTable(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
