package bio.terra.cli.command.config.set;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set image" command. */
@Command(name = "image", description = "Set the Docker image to use for launching applications.")
public class Image extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Image.class);

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ImageIdArgGroup argGroup;

  static class ImageIdArgGroup {
    @CommandLine.Option(names = "--image", description = "Docker image id or tag.")
    private String imageId;

    @CommandLine.Option(names = "--default", description = "Use the default image id or tag.")
    private boolean useDefault;
  }

  /** Updates the docker image id property of the global context. */
  @Override
  protected void execute() {
    logger.debug("terra config set image");
    Config config = Context.getConfig();
    String prevImageId = config.getDockerImageId();
    String newImageId = argGroup.useDefault ? Config.getDefaultImageId() : argGroup.imageId;
    config.setDockerImageId(newImageId);

    if (config.getDockerImageId().equals(prevImageId)) {
      OUT.println("Docker image: " + config.getDockerImageId() + " (UNCHANGED)");
    } else {
      OUT.println(
          "Docker image: " + config.getDockerImageId() + " (CHANGED FROM " + prevImageId + ")");
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
