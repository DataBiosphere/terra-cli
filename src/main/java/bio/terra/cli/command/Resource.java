package bio.terra.cli.command;

import bio.terra.cli.command.resource.AddRef;
import bio.terra.cli.command.resource.CheckAccess;
import bio.terra.cli.command.resource.Create;
import bio.terra.cli.command.resource.Credentials;
import bio.terra.cli.command.resource.Delete;
import bio.terra.cli.command.resource.Describe;
import bio.terra.cli.command.resource.List;
import bio.terra.cli.command.resource.ListTree;
import bio.terra.cli.command.resource.Mount;
import bio.terra.cli.command.resource.MoveToFolder;
import bio.terra.cli.command.resource.OpenConsole;
import bio.terra.cli.command.resource.Resolve;
import bio.terra.cli.command.resource.Unmount;
import bio.terra.cli.command.resource.Update;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra resource" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "resource",
    header = "Manage references and controlled resources in the workspace.",
    description =
        "Terra contains two types of cloud resources within workspaces: *controlled* and *referenced* resources. \n\n"
            + "A *controlled resource* is a cloud resource that is managed by Terra. Used for shared project storage and Terra-controlled analysis environments. \n\n"
            + "A *reference* points to an *external* cloud resource that is not managed by Terra. Used for input data references, or shared cloud resources that are managed outside of Terra. \n\n"
            + "The `check-access` command lets you see whether you have access to a particular resource. This is useful to ensure your account has access to all input data required for an analysis. \n\n"
            + "The list of resources in a workspace is maintained on the Terra Workspace Manager server (and the CLI caches this list of resources locally). "
            + "Third-party tools can access resource details via environment variables (e.g. for a resource named `mybucket`, the `$TERRA_mybucket` env variable will be populated with the bucket URL).",
    subcommands = {
      AddRef.class,
      CheckAccess.class,
      Credentials.class,
      Create.class,
      Delete.class,
      Describe.class,
      List.class,
      ListTree.class,
      Mount.class,
      MoveToFolder.class,
      OpenConsole.class,
      Resolve.class,
      Unmount.class,
      Update.class,
    })
public class Resource {}
