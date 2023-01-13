package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.workspace.model.Folder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource list-tree" command. */
@CommandLine.Command(
    name = "list-tree",
    description = "List all resources and folders in tree view.")
public class ListTree extends WsmBaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  private static final HashMap<UUID, ArrayList<UUID>> EDGES = new HashMap<>();
  private static final HashMap<UUID, String> ID_TO_NAME = new HashMap<>();
  private static final HashMap<UUID, Boolean> IS_FOLDER = new HashMap<>();
  private static final UUID ROOT = UUID.randomUUID();
  private static final String TERRA_FOLDER_ID_PROPERTY_KEY = "terra-folder-id";

  /** List the resources and folders in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // Get all resources and folders in the workspace and sort by name
    List<UFResource> resources =
        Context.requireWorkspace().listResources().stream()
            .sorted(Comparator.comparing(Resource::getName))
            .map(Resource::serializeToCommand)
            .toList();
    List<Folder> folders =
        Context.requireWorkspace().listFolders().stream()
            .sorted(Comparator.comparing(Folder::getDisplayName))
            .toList();

    // Create edges map for DFS and store name for each id. Display the folder before the resource.
    // Note: The intuitive algorithm doesn't handle drawing lines correctly, so use this algorithm
    // from GNU Tree utility implementation: https://github.com/kddnewton/tree/blob/main/Tree.java
    // See https://github.com/DataBiosphere/terra-cli/pull/329/files#r982639897
    for (Folder folder : folders) {
      UUID folderId = folder.getId();
      EDGES
          .computeIfAbsent(
              folder.getParentFolderId() != null ? folder.getParentFolderId() : ROOT,
              k -> new ArrayList<>())
          .add(folderId);
      ID_TO_NAME.put(folderId, folder.getDisplayName());
      IS_FOLDER.put(folderId, true);
    }

    for (UFResource resource : resources) {
      UUID resourceId = resource.id;
      UUID folderId =
          resource.properties.stream()
              .filter(x -> x.getKey().equals(TERRA_FOLDER_ID_PROPERTY_KEY))
              .findFirst()
              .map(value -> UUID.fromString(value.getValue()))
              .orElse(ROOT);
      EDGES.computeIfAbsent(folderId, k -> new ArrayList<>()).add(resourceId);
      ID_TO_NAME.put(resourceId, resource.name);
      IS_FOLDER.put(resourceId, false);
    }

    DFSWalk(ROOT, "");
  }

  // A DFS walk helper function to print out a tree view graph.
  private void DFSWalk(UUID parentUuid, String prefix) {
    if (EDGES.containsKey(parentUuid)) {
      ArrayList<UUID> edgeList = EDGES.get(parentUuid);
      for (int index = 0; index < edgeList.size(); index++) {
        UUID childUuid = edgeList.get(index);
        boolean isLast = index == edgeList.size() - 1;
        System.out.println(prefix + (isLast ? "└── " : "├── ") + ID_TO_NAME.get(childUuid));
        if (IS_FOLDER.get(childUuid)) {
          DFSWalk(childUuid, prefix + (isLast ? "    " : "│   "));
        }
      }
    }
  }
}
