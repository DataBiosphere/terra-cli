package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.util.DateTime;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.StorageClass;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the `terra resources` commands that handle controlled GCS buckets and specify lifecycle
 * rules.
 */
@Tag("unit")
public class GcsBucketLifecycle extends SingleWorkspaceUnit {
  @Override
  @BeforeEach
  protected void setupEachTime() throws IOException {
    super.setupEachTime();

    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
  }

  @Test
  @DisplayName("lifecycle action delete (condition age)")
  void deleteAction() throws IOException {
    String name = "delete_age";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionDelete(lifecycleRuleFromGCS);
    assertEquals(365, lifecycleRuleFromGCS.getCondition().getAge(), "condition age matches");
  }

  @Test
  @DisplayName("lifecycle action set storage class (condition age)")
  void setStorageClassAction() throws IOException {
    String name = "setStorageClass_age";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionSetStorageClass(lifecycleRuleFromGCS, StorageClass.ARCHIVE);
    assertEquals(124, lifecycleRuleFromGCS.getCondition().getAge(), "condition age matches");
  }

  @Test
  @DisplayName("lifecycle condition created before (action delete)")
  void createdBeforeCondition() throws IOException {
    String name = "delete_createdBefore";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionDelete(lifecycleRuleFromGCS);
    assertEquals(
        new DateTime("2011-01-14"),
        lifecycleRuleFromGCS.getCondition().getCreatedBefore(),
        "condition created before matches");
  }

  @Disabled // TODO (PF-506): enable this test once WSM supports this field
  @DisplayName("lifecycle condition custom time before (action delete)")
  void customTimeBeforeCondition() throws IOException {
    String name = "delete_customTimeBefore";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionDelete(lifecycleRuleFromGCS);
    assertEquals(
        new DateTime("2012-10-15"),
        lifecycleRuleFromGCS.getCondition().getCustomTimeBefore(),
        "condition custom time before matches");
  }

  @Disabled // TODO (PF-506): enable this test once WSM supports this field
  @DisplayName("lifecycle condition days since custom time (action delete)")
  void daysSinceCustomTimeCondition() throws IOException {
    String name = "delete_daysSinceCustomTime";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionDelete(lifecycleRuleFromGCS);
    assertEquals(
        5,
        lifecycleRuleFromGCS.getCondition().getDaysSinceCustomTime(),
        "condition days since custom time matches");
  }

  @Disabled // TODO (PF-506): enable this test once WSM supports this field
  @DisplayName("lifecycle condition days since noncurrent time (action delete)")
  void daysSinceNoncurrentTimeCondition() throws IOException {
    String name = "delete_daysSinceNoncurrentTime";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionDelete(lifecycleRuleFromGCS);
    assertEquals(
        35,
        lifecycleRuleFromGCS.getCondition().getDaysSinceNoncurrentTime(),
        "condition days since noncurrent time matches");
  }

  @Test
  @DisplayName("lifecycle condition is live (action set storage class)")
  void isLiveCondition() throws IOException {
    String name = "setStorageClass_isLive";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionSetStorageClass(lifecycleRuleFromGCS, StorageClass.NEARLINE);
    assertFalse(lifecycleRuleFromGCS.getCondition().getIsLive(), "condition is live matches");
  }

  @Test
  @DisplayName("lifecycle condition matches storage class (action set storage class)")
  void matchesStorageClassCondition() throws IOException {
    String name = "setStorageClass_matchesStorageClass";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionSetStorageClass(lifecycleRuleFromGCS, StorageClass.COLDLINE);
    List<StorageClass> matchesStorageClass =
        lifecycleRuleFromGCS.getCondition().getMatchesStorageClass();
    assertEquals(2, matchesStorageClass.size(), "condition matches storage class has correct size");
    assertTrue(
        matchesStorageClass.containsAll(Arrays.asList(StorageClass.STANDARD, StorageClass.ARCHIVE)),
        "condition matches storage class has correct elements");
  }

  @Disabled // TODO (PF-506): enable this test once WSM supports this field
  @DisplayName("lifecycle condition noncurrent time before (action set storage class)")
  void noncurrentTimeBeforeCondition() throws IOException {
    String name = "setStorageClass_noncurrentTimeBefore";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionDelete(lifecycleRuleFromGCS);
    assertEquals(
        new DateTime("2014-08-28"),
        lifecycleRuleFromGCS.getCondition().getNoncurrentTimeBefore(),
        "condition nonconcurrent time before matches");
  }

  @Test
  @DisplayName("lifecycle condition number of newer versions (action set storage class)")
  void numberOfNewerVerionsCondition() throws IOException {
    String name = "setStorageClass_numNewerVersions";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionSetStorageClass(lifecycleRuleFromGCS, StorageClass.STANDARD);
    assertEquals(
        54,
        lifecycleRuleFromGCS.getCondition().getNumberOfNewerVersions(),
        "condition number of newer versions matches");
  }

  @Test
  @DisplayName("auto-delete option")
  void autoDeleteOption() throws IOException {
    String resourceName = "autodelete";
    String bucketName = UUID.randomUUID().toString();
    int autoDelete = 24;

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName
    // --auto-delete=$autodelete --format=json`
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "gcs-bucket",
        "--name=" + resourceName,
        "--bucket-name=" + bucketName,
        "--auto-delete=" + autoDelete,
        "--format=json");

    List<? extends BucketInfo.LifecycleRule> lifecycleRulesFromGCS =
        getLifecycleRulesFromCloud(bucketName);
    assertEquals(1, lifecycleRulesFromGCS.size(), "bucket has exactly one lifecycle rule defined");
    BucketInfo.LifecycleRule lifecycleRuleFromGCS = lifecycleRulesFromGCS.get(0);

    expectActionDelete(lifecycleRuleFromGCS);
    assertEquals(autoDelete, lifecycleRuleFromGCS.getCondition().getAge(), "condition age matches");
  }

  @Test
  @DisplayName("multiple lifecycle conditions in one rule")
  void multipleConditions() throws IOException {
    String name = "multipleConditions";
    BucketInfo.LifecycleRule lifecycleRuleFromGCS =
        createBucketWithOneLifecycleRule(name, name + ".json");

    expectActionSetStorageClass(lifecycleRuleFromGCS, StorageClass.COLDLINE);
    assertEquals(68, lifecycleRuleFromGCS.getCondition().getAge(), "condition age matches");
    List<StorageClass> matchesStorageClass =
        lifecycleRuleFromGCS.getCondition().getMatchesStorageClass();
    assertEquals(1, matchesStorageClass.size(), "condition matches storage class has correct size");
    assertTrue(
        matchesStorageClass.containsAll(Arrays.asList(StorageClass.NEARLINE)),
        "condition matches storage class has correct elements");
    assertEquals(
        70,
        lifecycleRuleFromGCS.getCondition().getNumberOfNewerVersions(),
        "condition number of newer versions matches");
  }

  @Test
  @DisplayName("multiple lifecycle rules")
  void multipleRules() throws IOException {
    String name = "multipleRules";
    List<? extends BucketInfo.LifecycleRule> lifecycleRules =
        createBucketWithLifecycleRules(name, name + ".json");

    assertEquals(2, lifecycleRules.size(), "bucket has two lifecycle rules defined");

    Optional<? extends BucketInfo.LifecycleRule> ruleWithDeleteAction =
        lifecycleRules.stream()
            .filter(rule -> rule.getAction().getActionType().equals("Delete"))
            .findFirst();
    assertTrue(ruleWithDeleteAction.isPresent(), "one rule has action type = delete");
    expectActionDelete(ruleWithDeleteAction.get());
    assertEquals(84, ruleWithDeleteAction.get().getCondition().getAge(), "condition age matches");

    Optional<? extends BucketInfo.LifecycleRule> ruleWithSetStorageClassAction =
        lifecycleRules.stream()
            .filter(rule -> rule.getAction().getActionType().equals("SetStorageClass"))
            .findFirst();
    assertTrue(
        ruleWithSetStorageClassAction.isPresent(), "one rule has action type = set storage class");
    expectActionSetStorageClass(ruleWithSetStorageClassAction.get(), StorageClass.COLDLINE);
    assertFalse(
        ruleWithSetStorageClassAction.get().getCondition().getIsLive(),
        "condition is live matches");
  }

  /** Check that the action is Delete. */
  private void expectActionDelete(BucketInfo.LifecycleRule rule) {
    assertEquals("Delete", rule.getAction().getActionType(), "Delete action type matches");
  }

  /** Check that the action is SetStorageClass and the storage class is the given one. */
  private void expectActionSetStorageClass(
      BucketInfo.LifecycleRule rule, StorageClass storageClass) {
    assertEquals(
        "SetStorageClass", rule.getAction().getActionType(), "SetStorageClass action type matches");
    assertEquals(
        storageClass,
        ((BucketInfo.LifecycleRule.SetStorageClassLifecycleAction) rule.getAction())
            .getStorageClass(),
        "SetStorageClass action storage class matches");
  }

  /**
   * Helper method that:
   *
   * <p>- Creates a controlled GCS bucket resource with the specified lifecycle JSON file.
   *
   * <p>- Queries GCS directly for the lifecycle rules on the bucket.
   *
   * <p>- Expects that there is a single lifecycle rule, and returns it.
   */
  private BucketInfo.LifecycleRule createBucketWithOneLifecycleRule(
      String resourceName, String lifecycleFilename) throws IOException {
    List<? extends BucketInfo.LifecycleRule> lifecycleRules =
        createBucketWithLifecycleRules(resourceName, lifecycleFilename);

    // check that a single lifecycle rule is set
    assertEquals(1, lifecycleRules.size(), "bucket has exactly one lifecycle rule defined");
    return lifecycleRules.get(0);
  }

  /**
   * Helper method that:
   *
   * <p>- Creates a controlled GCS bucket resource with the specified lifecycle JSON file.
   *
   * <p>- Queries GCS directly for the lifecycle rules on the bucket, and returns them.
   */
  private List<? extends BucketInfo.LifecycleRule> createBucketWithLifecycleRules(
      String resourceName, String lifecycleFilename) throws IOException {
    String bucketName = UUID.randomUUID().toString();
    Path lifecycle = TestCommand.getPathForTestInput("gcslifecycle/" + lifecycleFilename);

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName
    // --lifecycle=$lifecycle --format=json`
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "gcs-bucket",
        "--name=" + resourceName,
        "--bucket-name=" + bucketName,
        "--lifecycle=" + lifecycle.toString(),
        "--format=json");

    return getLifecycleRulesFromCloud(bucketName);
  }

  /** Helper method to get the lifecycle rules on the bucket by querying GCS directly. */
  private List<? extends BucketInfo.LifecycleRule> getLifecycleRulesFromCloud(String bucketName)
      throws IOException {
    Bucket createdBucketOnCloud =
        ExternalGCSBuckets.getStorageClient(workspaceCreator.getCredentials()).get(bucketName);
    List<? extends BucketInfo.LifecycleRule> lifecycleRules =
        createdBucketOnCloud.getLifecycleRules();

    lifecycleRules.stream().forEach(System.out::println); // log to console
    assertNotNull(createdBucketOnCloud, "looking up bucket via GCS API succeeded");
    assertNotNull(lifecycleRules, "bucket has lifecycle rules defined");

    return lifecycleRules;
  }
}
