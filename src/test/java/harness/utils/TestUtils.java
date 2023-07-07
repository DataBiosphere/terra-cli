package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.UFResource;
import java.util.UUID;

public class TestUtils {
  // appendRandomNumber: definition copied from Workspace Manager
  public static String appendRandomNumber(String prefix) {
    // Can't have dash because BQ dataset names can't have dash.
    // Can't have underscore because for controlled buckets, GCP recommends not having underscore
    // in bucket name.
    String randomString = prefix + UUID.randomUUID();
    return randomString.replaceAll("[-_]", "");
  }

  public static <T extends UFResource, E extends UFResource> void assertResourceProperties(
      T expected, E actual, String src) {
    assertEquals(expected.id, actual.id, "resource id matches that in " + src);
    assertEquals(expected.name, actual.name, "resource name matches that in " + src);
    assertEquals(
        expected.description, actual.description, "resource description matches that in " + src);
    assertEquals(
        expected.resourceType, actual.resourceType, "resource type matches that in " + src);
    assertEquals(
        expected.stewardshipType,
        actual.stewardshipType,
        "stewardship type matches that in " + src);
    assertEquals(
        expected.cloningInstructions,
        actual.cloningInstructions,
        "resource cloningInstructions matches that in " + src);
    assertEquals(
        expected.accessScope, actual.accessScope, "resource accessScope matches that in " + src);
    assertEquals(expected.managedBy, actual.managedBy, "managed by matches that in " + src);
    assertEquals(expected.region, actual.region, "region matches that in " + src);
    assertEquals(
        expected.privateUserName,
        actual.privateUserName,
        "resource user name matches that in " + src);
    assertEquals(
        expected.privateUserRole,
        actual.privateUserRole,
        "resource user role matches that in " + src);
  }
}
