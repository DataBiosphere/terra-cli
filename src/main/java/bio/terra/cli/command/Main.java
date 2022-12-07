package bio.terra.cli.command;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.app.passthrough.Bq;
import bio.terra.cli.command.app.passthrough.Gcloud;
import bio.terra.cli.command.app.passthrough.Git;
import bio.terra.cli.command.app.passthrough.Gsutil;
import bio.terra.cli.command.app.passthrough.Nextflow;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.CloudPlatform;
import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

/**
 * This class corresponds to the top-level "terra" command. It is also the entry-point for the
 * picocli library.
 */
@Command(
    name = "terra",
    subcommands = {
      App.class,
      Auth.class,
      Bq.class,
      Config.class,
      Cromwell.class,
      Gcloud.class,
      Git.class,
      Group.class,
      Gsutil.class,
      Nextflow.class,
      Notebook.class,
      Resolve.class,
      Resource.class,
      Server.class,
      Spend.class,
      Status.class,
      User.class,
      Version.class,
      Workspace.class
    },
    description = "Terra CLI")
public class Main implements Runnable {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Main.class);

  // color scheme used by all commands
  private static final CommandLine.Help.ColorScheme colorScheme =
      new CommandLine.Help.ColorScheme.Builder()
          .commands(CommandLine.Help.Ansi.Style.bold)
          .options(CommandLine.Help.Ansi.Style.fg_yellow)
          .parameters(CommandLine.Help.Ansi.Style.fg_yellow)
          .optionParams(CommandLine.Help.Ansi.Style.italic)
          .errors(CommandLine.Help.Ansi.Style.fg_blue)
          .stackTraces(CommandLine.Help.Ansi.Style.italic)
          .build();

  /** List of user input command and arguments. */
  private static List<String> argList = List.of();

  /**
   * Create and execute the top-level command. Tests call this method instead of {@link
   * #main(String...)} so that the process isn't terminated.
   *
   * @param args command and arguments
   * @return process exit code
   */
  @VisibleForTesting
  public static int runCommand(String... args) {
    CommandLine cmd = new CommandLine(new Main());
    cmd.setExecutionStrategy(new CommandLine.RunLast());
    cmd.setExecutionExceptionHandler(new UserActionableAndSystemExceptionHandler());
    cmd.setColorScheme(colorScheme);
    cmd.setCaseInsensitiveEnumValuesAllowed(true);

    // set the output and error streams to the defaults: stdout, stderr
    // save pointers to these streams in a singleton class, so we can access them throughout the
    // codebase without passing them around
    UserIO.setupPrinting(cmd);

    // store a reference to command in the context, used to control options at runtime
    Context.setCommandLine(cmd);
    // configSubcommands(cmd);

    // delegate to the appropriate command class, or print the usage if no command was specified
    int exitCode = cmd.execute(args);

    // allow mixing options and parameters for all commands except the pass-through app commands.
    // this is because any options that follow the app command name should NOT be interpreted by the
    // Terra CLI, we want to pass those through to the app instead
    Map<String, CommandLine> subcommands = cmd.getSubcommands();
    subcommands.get("bq").setStopAtPositional(true);
    subcommands.get("gsutil").setStopAtPositional(true);
    subcommands.get("nextflow").setStopAtPositional(true);
    subcommands.get("app").getSubcommands().get("execute").setStopAtPositional(true);

    if (args.length == 0) {
      cmd.usage(cmd.getOut());
    }

    return exitCode;
  }

  private static void configSubcommands2(CommandLine cmd) {
    Map<String, CommandLine> subcommands = cmd.getSubcommands();

    CommandSpec workspaceCreateSpec =
        subcommands.get("workspace").getSubcommands().get("create").getCommandSpec();
    OptionSpec platformSpec = workspaceCreateSpec.findOption("--platform");

    java.util.List<CloudPlatform> supportedCloudPlatforms =
        Context.isSetServer() ? Context.getServer().getSupportedCloudPlatforms() : null;
    try {
      throw new Exception("TEST--> in function 3 - " + supportedCloudPlatforms.size());
    } catch (Exception e) {
      e.printStackTrace();
    }

    boolean hidePlatform = supportedCloudPlatforms == null || supportedCloudPlatforms.size() < 2;

    if (platformSpec.hidden() != hidePlatform) {
      try {
        throw new Exception("TEST--> in function 4a - " + platformSpec.hidden());
      } catch (Exception e) {
        e.printStackTrace();
      }

      OptionSpec newPlatformSpec = OptionSpec.builder(platformSpec).hidden(hidePlatform).build();
      workspaceCreateSpec.remove(platformSpec);
      workspaceCreateSpec.addOption(newPlatformSpec);
      try {
        throw new Exception(
            "TEST--> in function 4b - " + workspaceCreateSpec.findOption("--platform").hidden());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Main entry point into the CLI application. This creates and executes the top-level command,
   * sets the exit code and terminates the process.
   *
   * @param args from stdin
   */
  public static void main(String... args) {
    // Save the user input args so that {@link BaseCommand} can log the command and arguments being
    // executed.
    argList = Arrays.asList(args);
    // run the command
    int exitCode = runCommand(args);

    // set the exit code and terminate the process
    System.exit(exitCode);
  }

  /** Get the user input arguments */
  public static List<String> getArgList() {
    return argList;
  }

  /** Required method to implement Runnable, but not actually called by picocli. */
  @Override
  public void run() {}

  /**
   * Custom handler class that intercepts all exceptions.
   *
   * <p>There are three categories of exceptions. All print a message to stderr and log the
   * exception.
   *
   * <p>- UserActionable = user can fix
   *
   * <p>- System = user cannot fix, exception specifically thrown by CLI code
   *
   * <p>- Unexpected = user cannot fix, exception not thrown by CLI code
   *
   * <p>The System and Unexpected cases are very similar, except that the message on the system
   * exception might be more readable/relevant.
   */
  private static class UserActionableAndSystemExceptionHandler
      implements CommandLine.IExecutionExceptionHandler {

    // color scheme used for printing out system and unexpected errors
    // (there is only a single error style that you can define for all commands, and we are already
    // using that for user-actionable errors)
    private static final CommandLine.Help.ColorScheme systemAndUnexpectedErrorStyle =
        new CommandLine.Help.ColorScheme.Builder()
            .errors(CommandLine.Help.Ansi.Style.fg_red, CommandLine.Help.Ansi.Style.bold)
            .build();

    // exit codes to use for each type of exception thrown
    private static final int USER_ACTIONABLE_EXIT_CODE = 1;
    private static final int SYSTEM_EXIT_CODE = 2;
    private static final int UNEXPECTED_EXIT_CODE = 3;

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
      String errorMessage;
      CommandLine.Help.Ansi.Text formattedErrorMessage;
      int exitCode;
      boolean printPointerToLogFile;
      if (ex instanceof UserActionableException) {
        errorMessage = ex.getMessage();
        formattedErrorMessage =
            cmd.getColorScheme()
                .errorText(
                    Objects.requireNonNullElse(
                        errorMessage, ex.getClass().getName() + ": Error message not found."));
        exitCode = USER_ACTIONABLE_EXIT_CODE;
        printPointerToLogFile = false;
      } else if (ex instanceof SystemException) {
        errorMessage = ex.getMessage();
        formattedErrorMessage =
            systemAndUnexpectedErrorStyle.errorText("[ERROR] ").concat(errorMessage);
        exitCode = SYSTEM_EXIT_CODE;
        printPointerToLogFile = true;
      } else if (ex instanceof PassthroughException) {
        errorMessage = Optional.ofNullable(ex.getMessage()).orElse("");
        formattedErrorMessage = cmd.getColorScheme().errorText(errorMessage);
        exitCode = ((PassthroughException) ex).getExitCode();
        printPointerToLogFile = false;
      } else {
        errorMessage =
            "An unexpected error occurred in "
                + ex.getClass().getCanonicalName()
                + ": "
                + ex.getMessage();
        formattedErrorMessage =
            systemAndUnexpectedErrorStyle.errorText("[ERROR] ").concat(errorMessage);
        exitCode = UNEXPECTED_EXIT_CODE;
        printPointerToLogFile = true;
      }

      // print the error for the user
      cmd.getErr().println(formattedErrorMessage);
      if (printPointerToLogFile) {
        cmd.getErr()
            .println(
                cmd.getColorScheme()
                    .stackTraceText("See " + Context.getLogFile() + " for more information"));
      }

      // log the exact message that was printed to the console, for easier debugging
      logger.error(errorMessage, ex);

      // set the process return code
      return exitCode;
    }
  }
}
