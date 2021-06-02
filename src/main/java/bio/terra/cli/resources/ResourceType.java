package bio.terra.cli.resources;

import bio.terra.cli.Resource;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.cli.serialization.command.resources.CommandAiNotebook;
import bio.terra.cli.serialization.command.resources.CommandBqDataset;
import bio.terra.cli.serialization.command.resources.CommandGcsBucket;
import bio.terra.cli.serialization.disk.DiskResource;
import bio.terra.cli.serialization.disk.resources.DiskAiNotebook;
import bio.terra.cli.serialization.disk.resources.DiskBqDataset;
import bio.terra.cli.serialization.disk.resources.DiskGcsBucket;

public enum ResourceType {
  GCS_BUCKET(bio.terra.workspace.model.ResourceType.GCS_BUCKET),
  BQ_DATASET(bio.terra.workspace.model.ResourceType.BIG_QUERY_DATASET),
  AI_NOTEBOOK(bio.terra.workspace.model.ResourceType.AI_NOTEBOOK);

  private bio.terra.workspace.model.ResourceType wsmResourceType;

  ResourceType(bio.terra.workspace.model.ResourceType wsmResourceType) {
    this.wsmResourceType = wsmResourceType;
  }

  public bio.terra.workspace.model.ResourceType getWsmResourceType() {
    return wsmResourceType;
  }

  public CommandResource.Builder getCommandBuilder(Resource resource) {
    switch (this) {
      case GCS_BUCKET:
        return new CommandGcsBucket.Builder((GcsBucket) resource);
      case BQ_DATASET:
        return new CommandBqDataset.Builder((BqDataset) resource);
      case AI_NOTEBOOK:
        return new CommandAiNotebook.Builder((AiNotebook) resource);
      default:
        throw new IllegalArgumentException("Unexpected resource type: " + this);
    }
  }

  public DiskResource.Builder getDiskBuilder(Resource resource) {
    switch (this) {
      case GCS_BUCKET:
        return new DiskGcsBucket.Builder((GcsBucket) resource);
      case BQ_DATASET:
        return new DiskBqDataset.Builder((BqDataset) resource);
      case AI_NOTEBOOK:
        return new DiskAiNotebook.Builder((AiNotebook) resource);
      default:
        throw new IllegalArgumentException("Unexpected resource type: " + this);
    }
  }

  public Resource.Builder getBuilder(DiskResource diskResource) {
    switch (this) {
      case GCS_BUCKET:
        return new GcsBucket.Builder((DiskGcsBucket) diskResource);
      case BQ_DATASET:
        return new BqDataset.Builder((DiskBqDataset) diskResource);
      case AI_NOTEBOOK:
        return new AiNotebook.Builder((DiskAiNotebook) diskResource);
      default:
        throw new IllegalArgumentException("Unexpected resource type: " + this);
    }
  }
}
