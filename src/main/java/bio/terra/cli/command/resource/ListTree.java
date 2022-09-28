package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.workspace.model.Folder;
import bio.terra.workspace.model.Property;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource list-tree" command. */
@CommandLine.Command(
    name = "list-tree",
    description = "List all resources and folders in tree view.")
public class ListTree extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  private final HashMap<UUID, ArrayList<UUID>> edges = new HashMap<>();
  private final HashMap<UUID, String> idToName = new HashMap<>();
  private final HashMap<UUID, Boolean> isFolderMap = new HashMap<>();
  private final UUID root = UUID.randomUUID();
  private final String TERRA_FOLDER_ID = "terra-folder-id";

  /** List the resources in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // Get all resources and folders in the workspace and sort by name
    List<UFResource> resources =
        Context.requireWorkspace().listResourcesAndSync().stream()
            .sorted(Comparator.comparing(Resource::getName))
            .map(Resource::serializeToCommand)
            .collect(Collectors.toList());
    List<Folder> folders =
        Context.requireWorkspace().listFolders().stream()
            .sorted(Comparator.comparing(Folder::getDisplayName))
            .collect(Collectors.toList());

    // Create edges map for DFS and store name for each id.
    // Display the folder before the resource.
    for (Folder folder : folders) {
      UUID folderId = folder.getId();
      edges
          .computeIfAbsent(
              folder.getParentFolderId() != null ? folder.getParentFolderId() : root,
              k -> new ArrayList<>())
          .add(folderId);
      idToName.put(folderId, folder.getDisplayName());
      isFolderMap.put(folderId, true);
    }

    for (UFResource resource : resources) {
      UUID resourceId = resource.id;
      Optional<Property> property =
          resource.properties.stream().filter(x -> x.getKey().equals(TERRA_FOLDER_ID)).findFirst();
      edges
          .computeIfAbsent(
              property.map(value -> UUID.fromString(value.getValue())).orElse(root),
              k -> new ArrayList<>())
          .add(resourceId);
      idToName.put(resourceId, resource.name);
      isFolderMap.put(resourceId, false);
    }

    DFSWalk(root, "");
  }

  // A DFS walk helper function to print out a tree view graph. Inspired by the original
  // GNU Tree utility implementation: https://github.com/kddnewton/tree/blob/main/Tree.java
  private void DFSWalk(UUID parentUuid, String prefix) {
    if (edges.containsKey(parentUuid)) {
      ArrayList<UUID> edgeList = edges.get(parentUuid);
      for (int index = 0; index < edgeList.size(); index++) {
        UUID childUuid = edgeList.get(index);
        boolean isLast = index == edgeList.size() - 1;
        System.out.println(prefix + (isLast ? "└── " : "├── ") + idToName.get(childUuid));
        if (isFolderMap.get(childUuid)) {
          DFSWalk(childUuid, prefix + (isLast ? "    " : "│   "));
        }
      }
    }
  }
}
