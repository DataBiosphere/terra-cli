package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFFormatOptionConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "format-option", description = "Output format option.")
public class FormatOption extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  @Override
  protected void execute() {
    UFFormatOptionConfig formatOptionConfig =
        new UFFormatOptionConfig.Builder()
            .formatOption(Context.getConfig().getFormatOption())
            .build();
    formatOption.printReturnValue(formatOptionConfig, FormatOption::printText);
  }

  public static void printText(UFFormatOptionConfig returnValue) {
    OUT.println("[text, json] output format = " + returnValue.formatOption);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
