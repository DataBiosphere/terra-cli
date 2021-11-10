package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.Context;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;

public class FormatDefaultValueProvider implements IDefaultValueProvider {

  @Override
  public String defaultValue(ArgSpec argSpec) throws Exception {
    Format.FormatOptions formatOption = Context.getConfig().getFormatOption();
    return formatOption.toString();
  }
}
