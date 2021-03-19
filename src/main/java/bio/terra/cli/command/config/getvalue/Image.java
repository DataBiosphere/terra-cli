package bio.terra.cli.command.config.getvalue;

import static bio.terra.cli.command.config.set.Image.ReturnValue;

import bio.terra.cli.command.BaseCommand;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get image" command. */
@Command(name = "image", description = "Get the Docker image used for launching applications.")
public class Image extends BaseCommand<ReturnValue> {

  @Override
  public ReturnValue execute() {
    return new ReturnValue(globalContext.dockerImageId);
  }
}
