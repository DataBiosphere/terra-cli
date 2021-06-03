package bio.terra.cli.serialization.command;

import bio.terra.cli.Resource;
import bio.terra.cli.resources.AiNotebook;
import bio.terra.cli.resources.BqDataset;
import bio.terra.cli.resources.GcsBucket;
import bio.terra.cli.serialization.command.resources.CommandAiNotebook;
import bio.terra.cli.serialization.command.resources.CommandBqDataset;
import bio.terra.cli.serialization.command.resources.CommandGcsBucket;
import bio.terra.cli.utils.Printer;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ControlledResourceIamRole;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;

/**
 * External representation of a workspace resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Resource} class for a resource's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class CommandResource {
  public final UUID id;
  public final String name;
  public final String description;
  public final Resource.Type resourceType;
  public final StewardshipType stewardshipType;
  public final CloningInstructionsEnum cloningInstructions;
  public final AccessScope accessScope;
  public final ManagedBy managedBy;
  public final String privateUserName;
  public final List<ControlledResourceIamRole> privateUserRoles;

  /** Serialize an instance of the internal class to the command format. */
  public CommandResource(Resource internalObj) {
    this.id = internalObj.getId();
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.resourceType = internalObj.getResourceType();
    this.stewardshipType = internalObj.getStewardshipType();
    this.cloningInstructions = internalObj.getCloningInstructions();
    this.accessScope = internalObj.getAccessScope();
    this.managedBy = internalObj.getManagedBy();
    this.privateUserName = internalObj.getPrivateUserName();
    this.privateUserRoles = internalObj.getPrivateUserRoles();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   * Calls the appropriate sub-class constructor based on the resource type.
   */
  public static CommandResource serializeFromInternal(Resource internalObj) {
    Resource.Type resourceType = internalObj.getResourceType();
    switch (resourceType) {
      case GCS_BUCKET:
        return new CommandGcsBucket((GcsBucket) internalObj);
      case BQ_DATASET:
        return new CommandBqDataset((BqDataset) internalObj);
      case AI_NOTEBOOK:
        return new CommandAiNotebook((AiNotebook) internalObj);
      default:
        throw new IllegalArgumentException("Unexpected resource type: " + resourceType);
    }
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    OUT.println("Name:         " + name);
    OUT.println("Description:  " + description);
    OUT.println("Stewardship:  " + stewardshipType);
    OUT.println("Cloning:      " + cloningInstructions);

    if (stewardshipType.equals(StewardshipType.CONTROLLED)) {
      OUT.println("Access scope: " + accessScope);
      OUT.println("Managed by:   " + managedBy);

      if (accessScope.equals(AccessScope.PRIVATE_ACCESS)) {
        OUT.println("Private user: " + privateUserName);
      }
    }
  }
}
