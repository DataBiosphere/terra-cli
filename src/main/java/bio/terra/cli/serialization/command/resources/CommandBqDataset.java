package bio.terra.cli.serialization.command.resources;

import bio.terra.cli.resources.BqDataset;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CommandBqDataset.Builder.class)
public class CommandBqDataset extends CommandResource {
  public final String projectId;
  public final String datasetId;

  private CommandBqDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
  }

  /** Print out this object in text format. */
  public void print() {
    super.print();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCP project id: " + projectId);
    OUT.println("Big Query dataset id: " + datasetId);
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends CommandResource.Builder {
    private String projectId;
    private String datasetId;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /** Call the private constructor. */
    public CommandBqDataset build() {
      return new CommandBqDataset(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the command format. */
    public Builder(BqDataset internalObj) {
      super(internalObj);
      this.projectId = internalObj.getProjectId();
      this.datasetId = internalObj.getDatasetId();
    }
  }
}
