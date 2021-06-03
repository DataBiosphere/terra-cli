package bio.terra.cli.serialization.command.resources;

import bio.terra.cli.businessobject.resources.BqDataset;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.PrintStream;

/**
 * External representation of a workspace Big Query dataset resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link BqDataset} class for a dataset's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class CommandBqDataset extends CommandResource {
  public final String projectId;
  public final String datasetId;

  /** Serialize an instance of the internal class to the command format. */
  public CommandBqDataset(BqDataset internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();
  }

  /** Print out this object in text format. */
  public void print() {
    super.print();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCP project id: " + projectId);
    OUT.println("Big Query dataset id: " + datasetId);
  }
}
