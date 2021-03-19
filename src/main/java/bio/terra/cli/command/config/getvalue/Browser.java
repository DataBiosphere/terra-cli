package bio.terra.cli.command.config.getvalue;

import static bio.terra.cli.command.config.set.Browser.ReturnValue;

import bio.terra.cli.command.BaseCommand;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get browser" command. */
@Command(
    name = "browser",
    description = "Check whether a browser is launched automatically during the login process.")
public class Browser extends BaseCommand<ReturnValue> {

  @Override
  public ReturnValue execute() {
    return new ReturnValue(globalContext.browserLaunchOption);
  }
}
