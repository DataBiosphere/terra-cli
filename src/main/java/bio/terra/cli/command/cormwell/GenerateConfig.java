package bio.terra.cli.command.cormwell;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.util.List;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app execute" command. */
@Command(
    name = "generate-config",
    description =
        "[FOR DEBUG] Execute a command in the application container for the Terra workspace, with no setup.")
public class GenerateConfig extends BaseCommand {

  // @CommandLine.Parameters(
  //     index = "0",
  //     paramLabel = "command",
  //     description = "Command to execute, including arguments",
  //     arity = "1..*")
  // private List<String> command;

  // @CommandLine.Option(names = "–execution-bucket", description = "GCS bucket to use.")
  // private String bucketName;
  //
  // @CommandLine.Option(
  //     names = "--enable-mysql",
  //     defaultValue = "false",
  //     description = "whether to enable mySQL.")
  // private boolean enableMysql;

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(List.of("generate.sh"));
  }
}
