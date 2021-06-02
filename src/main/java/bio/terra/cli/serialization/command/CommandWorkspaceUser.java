package bio.terra.cli.serialization.command;

import bio.terra.cli.WorkspaceUser;
import bio.terra.cli.utils.Printer;
import bio.terra.workspace.model.IamRole;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

public class CommandWorkspaceUser {
  public final String email;
  public final List<IamRole> roles;

  public CommandWorkspaceUser(WorkspaceUser internalObj) {
    this.email = internalObj.getEmail();
    this.roles = internalObj.getRoles();
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    List<String> rolesStr = roles.stream().map(IamRole::toString).collect(Collectors.toList());
    OUT.println(email + ": " + String.join(",", rolesStr));
  }
}
