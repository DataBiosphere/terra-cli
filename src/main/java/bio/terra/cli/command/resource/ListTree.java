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
import java.util.stream.Collectors;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource list-tree" command. */
@CommandLine.Command(
    name = "list-tree",
    description = "List all resources and folders in tree view.")
public class ListTree extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  private final HashMap<String, ArrayList<String>> edges = new HashMap<>();
  private final HashMap<String, String> nameMap = new HashMap<>();
  private final HashMap<String, Boolean> isFolderMap = new HashMap<>();

  /** List the resources in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // Get all resources and folders in the workspace
    List<UFResource> resources =
        Context.requireWorkspace().listResourcesAndSync().stream()
            .sorted(Comparator.comparing(Resource::getName))
            .map(Resource::serializeToCommand)
            .collect(Collectors.toList());
    List<Folder> folders = Context.requireWorkspace().listFolders();

    // Create edges map for DFS and store name for each id.
    // Display the folder before the resource.
    for (Folder folder : folders) {
      String folderId = folder.getId().toString();
      edges
          .computeIfAbsent(
              folder.getParentFolderId() != null ? folder.getParentFolderId().toString() : "root",
              k -> new ArrayList<>())
          .add(folderId);
      nameMap.put(folderId, folder.getDisplayName());
      isFolderMap.put(folderId, true);
    }

    for (UFResource resource : resources) {
      String resourceId = resource.id.toString();
      Optional<Property> property =
          resource.properties.stream()
              .filter(x -> x.getKey().equals("terra-folder-id"))
              .collect(Collectors.reducing((a, b) -> null));
      edges
          .computeIfAbsent(
              property.isPresent() ? property.get().getValue() : "root", k -> new ArrayList<>())
          .add(resourceId);
      nameMap.put(resourceId, resource.name);
      isFolderMap.put(resourceId, false);
    }

    DFSWalk("root", "");
  }

  // A DFS walk helper function to print out a tree view graph. Inspired by the original
  // GNU Tree utility implementation: https://github.com/kddnewton/tree/blob/main/Tree.java
  private void DFSWalk(String parentUuid, String prefix) {
    if (edges.containsKey(parentUuid)) {
      ArrayList<String> edgeList = edges.get(parentUuid);
      for (int index = 0; index < edgeList.size(); index++) {
        String childUuid = edgeList.get(index);
        if (index == edgeList.size() - 1) {
          System.out.println(prefix + "└── " + nameMap.get(childUuid));
          if (isFolderMap.get(childUuid)) {
            DFSWalk(childUuid, prefix + "    ");
          }
        } else {
          System.out.println(prefix + "├── " + nameMap.get(childUuid));
          if (isFolderMap.get(childUuid)) {
            DFSWalk(childUuid, prefix + "│   ");
          }
        }
      }
    }
  }
}
