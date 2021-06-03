package bio.terra.cli.serialization.command.resources;

import bio.terra.cli.resources.GcsBucket;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.PrintStream;

/**
 * External representation of a workspace GCS bucket resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GcsBucket} class for a bucket's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class CommandGcsBucket extends CommandResource {
  public final String bucketName;

  /** Serialize an instance of the internal class to the command format. */
  public CommandGcsBucket(GcsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
  }

  /** Print out this object in text format. */
  public void print() {
    super.print();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCS bucket name: " + bucketName);
  }
}
