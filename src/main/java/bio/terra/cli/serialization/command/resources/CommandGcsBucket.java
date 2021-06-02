package bio.terra.cli.serialization.command.resources;

import bio.terra.cli.resources.GcsBucket;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CommandGcsBucket.Builder.class)
public class CommandGcsBucket extends CommandResource {
  public final String bucketName;

  private CommandGcsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
  }

  /** Print out this object in text format. */
  public void print() {
    super.print();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCS bucket name: " + bucketName);
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends CommandResource.Builder {
    private String bucketName;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    /** Call the private constructor. */
    public CommandGcsBucket build() {
      return new CommandGcsBucket(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the command format. */
    public Builder(GcsBucket internalObj) {
      super(internalObj);
      this.bucketName = internalObj.getBucketName();
    }
  }
}
