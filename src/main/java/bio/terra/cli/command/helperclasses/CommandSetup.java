package bio.terra.cli.command.helperclasses;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.io.PrintStream;
import java.util.concurrent.Callable;

public abstract class CommandSetup implements Callable<Integer> {
  protected GlobalContext globalContext;
  protected WorkspaceContext workspaceContext;

  // output stream to use for writing command return value
  protected static final PrintStream OUT = System.out;

  @Override
  public Integer call() {
    // read in the global and workspace context
    globalContext = GlobalContext.readFromFile();
    workspaceContext = WorkspaceContext.readFromFile();

    // do the login flow if required
    if (doLogin()) {
      new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    }

    // execute the command
    execute();

    // set the command exit code
    return 0;
  }

  /** Required override for executing this command and printing any output. */
  protected abstract void execute();

  /**
   * This method returns true if login is required for the command. Default implementation is to
   * always require login.
   *
   * <p>Sub-classes can use the current global and workspace context to decide whether to require
   * login.
   *
   * @return true if login is required for this command
   */
  protected boolean doLogin() {
    return true;
  }
}
