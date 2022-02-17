package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get image" command. */
@Command(name = "image", description = "Get the Docker image used for launching applications.")
public class Image extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Return the docker image id property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue("getting docker image");
    formatOption.printReturnValue(Context.getConfig().getDockerImageId());
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
