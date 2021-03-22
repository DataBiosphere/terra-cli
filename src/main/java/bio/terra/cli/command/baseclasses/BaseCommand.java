package bio.terra.cli.command.baseclasses;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * Base class for all commands. This class handles:
 *
 * <p>- reading in the current global and workspace context
 *
 * <p>- setting up logging
 *
 * <p>- executing the command
 *
 * <p>- printing the return value
 *
 * <p>Sub-classes define how to execute the command (i.e. the implementation of {@link #execute}).
 *
 * @param <T> class type of the command's return value
 */
public abstract class BaseCommand<T> implements Callable<Integer> {
  protected GlobalContext globalContext;
  protected WorkspaceContext workspaceContext;

  // output stream to use for writing command return value
  protected static final PrintStream out = System.out;

  @Override
  public Integer call() {
    // read in the global and workspace context
    globalContext = GlobalContext.readFromFile();
    workspaceContext = WorkspaceContext.readFromFile();

    // do the login flow if required
    if (doLogin()) {
      new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    }

    // get the return value object
    T returnValue = execute();

    // print the return value
    printReturnValue(returnValue);

    // set the command exit code
    return 0;
  }

  /**
   * Required override for executing this command and returning an instance of the return value
   * object.
   *
   * @return command return value
   */
  protected abstract T execute();

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

  /**
   * Default implementation of printing the return value. This method uses the {@link
   * Object#toString} method of the return value object.
   *
   * <p>Skips printing out a null value to handle the no return value case (i.e. class type
   * parameter T = Void).
   *
   * @param returnValue command return value
   */
  protected void printReturnValue(T returnValue) {
    if (returnValue != null) {
      out.println(returnValue.toString());
    }
  }
}
