package bio.terra.cli.command.auth;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth revoke" command. */
@Command(name = "revoke", description = "Revoke credentials from an account.")
public class Revoke implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    System.out.println("terra auth revoke");
    return 0;
  }
}
