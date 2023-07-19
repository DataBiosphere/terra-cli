package bio.terra.cli.command.folder;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.workspace.model.Folder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra folder tree" command. */
@Command(name = "tree", description = "show the folder hierarchy.")
public class Tree extends WsmBaseCommand {

  /** Map for folder id to folder. */
  private static final Map<UUID, Folder> ID_TO_FOLDER = new HashMap<>();
  /** Map for folder to all of its children folders. */
  private static final Map<Folder, ArrayList<Folder>> EDGES = new HashMap<>();

  /** An imaginary root folder of a workspace. */
  private static final Folder ROOT = new Folder().id(UUID.randomUUID()).displayName("root");

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** List the resources and folders in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // Get folders in the workspace and sort by name
    Context.requireWorkspace().listFolders().forEach(f -> ID_TO_FOLDER.put(f.getId(), f));

    // Create edges map for DFS and store name for each id. Display the folder before the resource.
    // Note: The intuitive algorithm doesn't handle drawing lines correctly, so use this algorithm
    // from GNU Tree utility implementation: https://github.com/kddnewton/tree/blob/main/Tree.java
    // See https://github.com/DataBiosphere/terra-cli/pull/329/files#r982639897
    for (Folder folder : ID_TO_FOLDER.values()) {
      EDGES
          .computeIfAbsent(
              folder.getParentFolderId() != null
                  ? ID_TO_FOLDER.get(folder.getParentFolderId())
                  : ROOT,
              k -> new ArrayList<>())
          .add(folder);
    }

    dfsWalk(ROOT, "");
  }

  // A DFS walk helper function to print out a tree view graph.
  private static void dfsWalk(Folder parentFolder, String prefix) {
    if (EDGES.containsKey(parentFolder)) {
      ArrayList<Folder> edgeList = EDGES.get(parentFolder);
      for (int index = 0; index < edgeList.size(); index++) {
        Folder childFolder = edgeList.get(index);
        boolean isLast = index == edgeList.size() - 1;
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(childFolder.getDisplayName());
        sb.append(" (");
        sb.append(childFolder.getId());
        sb.append(")");
        System.out.println(sb);
        dfsWalk(childFolder, prefix + (isLast ? "    " : "│   "));
      }
    }
  }
}
