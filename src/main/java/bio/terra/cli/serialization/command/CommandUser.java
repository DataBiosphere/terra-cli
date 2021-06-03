package bio.terra.cli.serialization.command;

import bio.terra.cli.User;
import bio.terra.cli.utils.Printer;
import java.io.PrintStream;

/**
 * External representation of a user for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link User} class for a user's internal representation.
 */
public class CommandUser {
  public final String id;
  public final String email;
  public final String proxyGroupEmail;
  public final boolean loggedIn;

  /** Serialize an instance of the internal class to the disk format. */
  public CommandUser(User internalObj) {
    this.id = internalObj.getId();
    this.email = internalObj.getEmail();
    this.proxyGroupEmail = internalObj.getProxyGroupEmail();
    this.loggedIn = internalObj.requiresReauthentication();
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    OUT.println("User email: " + email);
    OUT.println("Proxy group email: " + proxyGroupEmail);
    OUT.println("LOGGED " + (loggedIn ? "IN" : "OUT"));
  }
}
