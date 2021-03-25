package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value image" command. */
@Command(name = "image", description = "Get the Docker image used for launching applications.")
public class Image extends BaseCommand {

  @CommandLine.Mixin FormatOption formatOption;

  /** Return the docker image id property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(globalContext.dockerImageId);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
