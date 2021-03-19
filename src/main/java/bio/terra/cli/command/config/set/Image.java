package bio.terra.cli.command.config.set;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.command.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set image" command. */
@Command(
    name = "image",
    description = "[FOR DEBUG] Set the Docker image to use for launching applications.")
public class Image extends BaseCommand<Image.ReturnValue> {

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ImageIdArgGroup argGroup;

  static class ImageIdArgGroup {
    @CommandLine.Option(names = "--image", description = "image id or tag")
    private String imageId;

    @CommandLine.Option(names = "--default", description = "use the default image id or tag")
    private boolean useDefault;
  }

  @Override
  public ReturnValue execute() {
    String newImageId = argGroup.useDefault ? DockerAppsRunner.defaultImageId() : argGroup.imageId;
    new DockerAppsRunner(globalContext, workspaceContext).updateImageId(newImageId);

    return new ReturnValue(newImageId);
  }

  /**
   * The return value for this command is just the current value of the docker image id in the
   * global context.
   */
  public static class ReturnValue extends BaseCommand.BaseReturnValue {
    public String imageId;

    public ReturnValue(String imageId) {
      this.imageId = imageId;
    }

    @Override
    public void printText() {
      output.println("Docker image id for running apps = " + imageId);
    }
  }
}
