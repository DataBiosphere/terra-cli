package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.serialization.userfacing.UFFormatConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get format" command. */
@Command(name = "format", description = "Get the default output format option.")
public class Format extends BaseCommand {
  @CommandLine.Mixin bio.terra.cli.command.shared.options.Format formatOption;

  @Override
  protected void execute() {
    UFFormatConfig formatOptionConfig =
        new UFFormatConfig.Builder().formatOption(Context.getConfig().getFormat()).build();
    formatOption.printReturnValue(formatOptionConfig, Format::printText);
  }

  public static void printText(UFFormatConfig returnValue) {
    OUT.println("[text, json] output format = " + returnValue.format);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
