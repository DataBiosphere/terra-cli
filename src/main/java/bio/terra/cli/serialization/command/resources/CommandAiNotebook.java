package bio.terra.cli.serialization.command.resources;

import bio.terra.cli.businessobject.resources.AiNotebook;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.api.services.notebooks.v1.model.Instance;
import java.io.PrintStream;

/**
 * External representation of a workspace AI notebook resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AiNotebook} class for a notebook's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class CommandAiNotebook extends CommandResource {
  public final String projectId;
  public final String instanceId;
  public final String location;
  public final String instanceName;
  public final String state;
  public final String proxyUri;
  public final String createTime;

  /** Serialize an instance of the internal class to the command format. */
  public CommandAiNotebook(AiNotebook internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.instanceId = internalObj.getInstanceId();
    this.location = internalObj.getLocation();

    Instance instance = internalObj.getInstance();
    this.instanceName = instance.getName();
    this.state = instance.getState();
    this.proxyUri = instance.getProxyUri();
    this.createTime = instance.getCreateTime();
  }

  /** Print out this object in text format. */
  public void print() {
    super.print();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCP project id:                " + projectId);
    OUT.println("AI Notebook instance location: " + location);
    OUT.println("AI Notebook instance id:       " + instanceId);
    OUT.println("Instance name: " + instanceName);
    OUT.println("State:         " + state);
    OUT.println("Proxy URL:     " + proxyUri);
    OUT.println("Create time:   " + createTime);
  }
}
