package bio.terra.cli.command.app;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app set-image" command. */
@Command(
    name = "set-image",
    description = "[FOR DEBUG] Set the Docker image to use for launching applications.")
public class SetImage implements Callable<Integer> {

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ImageIdArgGroup argGroup;

  static class ImageIdArgGroup {
    @CommandLine.Option(names = "--image", description = "image id or tag")
    private String imageId;

    @CommandLine.Option(names = "--default", description = "use the default image id or tag")
    private boolean useDefault;
  }

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    String newImageId = argGroup.useDefault ? DockerAppsRunner.defaultImageId() : argGroup.imageId;

    String prevImageId = globalContext.dockerImageId;
    new DockerAppsRunner(globalContext, workspaceContext).updateImageId(newImageId);

    if (globalContext.dockerImageId.equals(prevImageId)) {
      System.out.println("Docker image: " + globalContext.dockerImageId + " (UNCHANGED)");
    } else {
      System.out.println(
          "Docker image: " + globalContext.dockerImageId + " (CHANGED FROM " + prevImageId + ")");
    }

    return 0;
  }
}
