package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Server;
import bio.terra.cli.utils.Printer;
import java.io.PrintStream;

/**
 * External representation of a server for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Server} class for a server's internal representation.
 */
public class UFServer {
  public final String name;
  public final String description;
  public final String samUri;
  public final String workspaceManagerUri;
  public final String dataRepoUri;

  /** Serialize an instance of the internal class to the command format. */
  public UFServer(Server internalObj) {
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.samUri = internalObj.getSamUri();
    this.workspaceManagerUri = internalObj.getWorkspaceManagerUri();
    this.dataRepoUri = internalObj.getDataRepoUri();
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    OUT.println(name + ": " + description);
  }
}
