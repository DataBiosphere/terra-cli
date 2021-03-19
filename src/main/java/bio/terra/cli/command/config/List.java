package bio.terra.cli.command.config;

import bio.terra.cli.command.BaseCommand;
import bio.terra.cli.command.config.set.Browser;
import bio.terra.cli.command.config.set.Image;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra config list" command. */
@CommandLine.Command(
    name = "list",
    description = "List all configuration properties and their values.")
public class List extends BaseCommand<List.ReturnValue> {

  @Override
  public ReturnValue execute() {
    Browser.ReturnValue browser =
        new bio.terra.cli.command.config.getvalue.Browser().getReturnValue();
    Image.ReturnValue image = new bio.terra.cli.command.config.getvalue.Image().getReturnValue();

    return new ReturnValue(browser, image);
  }

  /** The return value for this command is a combination of all current config values. */
  public static class ReturnValue extends BaseCommand.BaseReturnValue {
    public Browser.ReturnValue browser;
    public Image.ReturnValue image;

    public ReturnValue(Browser.ReturnValue browser, Image.ReturnValue image) {
      this.browser = browser;
      this.image = image;
    }

    @Override
    public void printText() {
      browser.printText();
      image.printText();
    }
  }
}
