package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.UFResource;
import java.util.Random;

public class TestUtils {
  private static final Random RANDOM = new Random();

  public static String appendRandomNumber(String string) {
    // This might be used for BQ datasets, so can't have "-". (BQ dataset names can't have "-".)
    return string + RANDOM.nextInt(10000);
  }

  public static <T extends UFResource, E extends UFResource> void assertResourceProperties(
      T expected, E actual, String src) {
    assertEquals(expected.id, actual.id, "resource id matches that in " + src);
    assertEquals(expected.name, actual.name, "resource name matches that in " + src);
    assertEquals(
        expected.resourceType, actual.resourceType, "resource type matches that in " + src);
    assertEquals(
        expected.accessScope, actual.accessScope, "resource accessScope matches that in " + src);
    assertEquals(
        expected.cloningInstructions,
        actual.cloningInstructions,
        "resource cloningInstructions matches that in " + src);
    assertEquals(
        expected.description, actual.description, "resource description matches that in " + src);
    assertEquals(
        expected.privateUserName,
        actual.privateUserName,
        "resource user name matches that in " + src);
  }
}
