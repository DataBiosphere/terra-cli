package harness.utils;

import harness.TestCommand;
import harness.TestUsers;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Utility class for tracking groups created by a test class, and then trying to delete them all at
 * the end.
 */
public class SamGroups {
  // keep a map of groups created by tests, so we can try to clean them up
  // group name -> test user that created it
  private Map<String, TestUsers> trackedGroups = new HashMap<>();

  private static final Random RANDOM = new Random();

  /** Try to delete each group that was tracked here. */
  public void deleteAllTrackedGroups() throws IOException {
    for (Map.Entry<String, TestUsers> groupCreated : trackedGroups.entrySet()) {
      groupCreated.getValue().login();
      TestCommand.Result cmd =
          TestCommand.runCommand("group", "delete", "--name=" + groupCreated.getKey(), "--quiet");
      if (cmd.exitCode == 0) {
        // log if a test didn't clean up a group
        System.out.println("group was not cleaned up by test: " + groupCreated.getKey());
      }
    }
  }

  /** Add a group to be tracked. */
  public void trackGroup(String name, TestUsers admin) {
    trackedGroups.put(name, admin);
  }

  /** Helper method to generate a random group name. */
  public static String randomGroupName() {
    return "terraCLI_" + RANDOM.nextInt(Integer.MAX_VALUE);
  }

  public Map<String, TestUsers> getTrackedGroups() {
    return trackedGroups;
  }
}
