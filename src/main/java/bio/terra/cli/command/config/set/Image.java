package bio.terra.cli.command.config.set;

import bio.terra.cli.apps.DockerCommandRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set image" command. */
@Command(name = "image", description = "Set the Docker image to use for launching applications.")
public class Image extends BaseCommand {

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ImageIdArgGroup argGroup;

  static class ImageIdArgGroup {
    @CommandLine.Option(names = "--image", description = "image id or tag")
    private String imageId;

    @CommandLine.Option(names = "--default", description = "use the default image id or tag")
    private boolean useDefault;
  }

  /** Updates the docker image id property of the global context. */
  @Override
  protected void execute() {
    String prevImageId = globalContext.getDockerImageId();
    String newImageId =
        argGroup.useDefault ? DockerCommandRunner.defaultImageId() : argGroup.imageId;
    new DockerCommandRunner().updateImageId(newImageId);

    if (globalContext.getDockerImageId().equals(prevImageId)) {
      OUT.println("Docker image: " + globalContext.getDockerImageId() + " (UNCHANGED)");
    } else {
      OUT.println(
          "Docker image: "
              + globalContext.getDockerImageId()
              + " (CHANGED FROM "
              + prevImageId
              + ")");
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
