package harness.utils;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceUser;
import bio.terra.workspace.model.IamRole;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestContext;
import harness.TestUsers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This class implements a script to cleanup workspaces owned by test users. Tests are not supposed
 * to leak workspaces, but if they do, the logging in this class should help track down any
 * offenders. During development, in particular, workspace leaks are more likely, so this is a
 * backup cleanup mechanism for those.
 *
 * <p>The main reason we don't want to leave workspaces around is because it increases the number of
 * Google groups to which a test user belongs. There is a hard upper limit on the number of groups a
 * user can belong to, so we want to make sure not to gradually approach the limit over time (e.g.
 * as happened with william.thunderlord).
 *
 * <p>This script is written in Java, instead of Bash, because that's where the test users are
 * defined, so it's easier to loop through them here.
 */
public class CleanupTestUserWorkspaces {
  private static List<UUID> deletedWorkspaces = new ArrayList<>();
  private static List<UUID> failedWorkspaces = new ArrayList<>();

  /**
   * List all workspaces the test user has access to and try to delete each one that the test user
   * owns. Deletes up to 100 workspaces at a time.
   */
  private static void deleteWorkspaces(TestUsers testUser, boolean isDryRun) throws IOException {
    System.out.println("Deleting workspaces for testuser " + testUser.email);
    TestContext.clearGlobalContextDir();
    testUser.login();

    // `terra workspace list`
    List<UFWorkspace> listWorkspaces =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--limit=100");

    List<UFWorkspaceUser> listWorkspaceUsers;
    for (UFWorkspace workspace : listWorkspaces) {
      try {
        // `terra workspace list-users`
        listWorkspaceUsers =
            TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "workspace", "list-users", "--workspace=" + workspace.id);

        // find the user in the list
        Optional<UFWorkspaceUser> workspaceUser =
            listWorkspaceUsers.stream()
                .filter(user -> user.email.equalsIgnoreCase(testUser.email))
                .findAny();
        // skip deleting if the test user is not an owner
        if (workspaceUser.isEmpty() || !workspaceUser.get().roles.contains(IamRole.OWNER)) {
          System.out.println(
              "Skip deleting workspace because test user is not an owner: id="
                  + workspace.id
                  + ", testuser="
                  + testUser.email);
          continue;
        }

        System.out.println(
            "Deleting workspace: id=" + workspace.id + ", testuser=" + testUser.email);
        if (!isDryRun) {
          // `terra workspace delete --workspace=$id`
          TestCommand.runCommandExpectSuccess(
              "workspace", "delete", "--workspace=" + workspace.id, "--quiet");
          System.out.println(
              "Cleaned up workspace: id=" + workspace.id + ", testuser=" + testUser.email);
        }
        deletedWorkspaces.add(workspace.id);
      } catch (Throwable ex) {
        System.out.println(
            "Error deleting workspace: id=" + workspace.id + ", testuser=" + testUser.email);
        ex.printStackTrace();
        failedWorkspaces.add(workspace.id);
        continue;
      }
    }

    // `terra auth revoke`
    TestCommand.runCommandExpectSuccess("auth", "revoke");
  }

  /**
   * Loop through all test users with spend profile access and try to delete all workspaces they
   * own.
   */
  public static void main(String... args) {
    // (see the Gradle cleanupTestUserWorkspaces task for how these System properties get set from
    // Gradle properties)
    String server = System.getProperty("TERRA_SERVER");
    if (server == null || server.isEmpty()) {
      throw new UserActionableException("Specify the server to cleanup.");
    }
    TestCommand.runCommandExpectSuccess("server", "set", "--name", server);

    boolean isDryRun = Boolean.parseBoolean(System.getProperty("DRY_RUN"));

    System.out.println("TERRA_SERVER: " + server);
    System.out.println("DRY_RUN: " + isDryRun);

    Arrays.stream(TestUsers.values())
        .filter(TestUsers::hasSpendAccess)
        .forEach(
            testUser -> {
              try {
                deleteWorkspaces(testUser, isDryRun);
              } catch (IOException e) {
                System.out.println("Error cleaning up workspaces for testuser: " + testUser.email);
              }
            });

    System.out.println("Deleted workspaces:");
    deletedWorkspaces.forEach(
        workspaceId -> {
          System.out.println("  " + workspaceId);
        });
    System.out.println("Failed workspaces:");
    failedWorkspaces.forEach(
        workspaceId -> {
          System.out.println("  " + workspaceId);
        });
  }
}
