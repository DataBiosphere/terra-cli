package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.Printer;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * External representation of a workspace for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Workspace} class for a workspace's internal representation.
 */
public class UFWorkspace {
  public final UUID id;
  public final String name;
  public final String description;
  public final String googleProjectId;
  public final String serverName;
  public final String userEmail;
  public final List<UFResource> resources;

  /** Serialize an instance of the internal class to the disk format. */
  public UFWorkspace(Workspace internalObj) {
    this.id = internalObj.getId();
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.googleProjectId = internalObj.getGoogleProjectId();
    this.serverName = internalObj.getServerName();
    this.userEmail = internalObj.getUserEmail();
    this.resources =
        internalObj.getResources().stream()
            .map(Resource::serializeToCommand)
            .collect(Collectors.toList());
  }

  /** Print out a workspace object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    OUT.println("Terra workspace id: " + id);
    OUT.println("Display name: " + name);
    OUT.println("Description: " + description);
    OUT.println("Google project: " + googleProjectId);
    OUT.println(
        "Cloud console: https://console.cloud.google.com/home/dashboard?project="
            + googleProjectId);
  }
}
