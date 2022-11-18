package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.GcpGcsBucketLifecycleRuleActionType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * This POJO class specifies a list of GCS bucket lifecycle rules. Its structure mimics the one used
 * by the `gsutil lifecycle` command.
 *
 * <p>Command ref: https://cloud.google.com/storage/docs/gsutil/commands/lifecycle
 *
 * <p>GCS bucket JSON format ref: https://cloud.google.com/storage/docs/json_api/v1/buckets#resource
 *
 * <p>This class is intended for passing throughout the CLI codebase, and also for exposing to users
 * so that they can specify the lifecycle rules in a JSON file and pass that to the CLI.
 */
@SuppressFBWarnings(
    value = {"UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"},
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class AwsBucketLifecycle {
  public List<Rule> rule = new ArrayList<>();

  /**
   * Helper method to build a lifecycle rule that auto-deletes the contents of the bucket after some
   * number of days.
   *
   * @param numDays number of days after which to delete an object in the bucket
   * @return lifecycle rule definition
   */
  public static AwsBucketLifecycle buildAutoDeleteRule(Integer numDays) {
    Action action = new Action();
    action.type = ActionType.Delete;

    Condition condition = new Condition();
    condition.age = numDays;

    Rule rule = new Rule();
    rule.action = action;
    rule.condition = condition;

    AwsBucketLifecycle lifecycle = new AwsBucketLifecycle();
    lifecycle.rule.add(rule);

    return lifecycle;
  }

  /**
   * This enum defines the possible ActionTypes for bucket lifecycle rules, and maps these types to
   * the corresponding {@link GcpGcsBucketLifecycleRuleActionType} enum in the WSM client library.
   * The CLI defines its own version of this enum so that:
   *
   * <p>- The CLI can use different enum names. In this case, the publicly documented GCS resource
   * definition uses a slightly different casing of the enum values. In an effort to mimic the
   * similar `gsutil lifecycle` command as closely as possible, the CLI will use the GCS casing,
   * instead of the WSM client library version.
   * (https://cloud.google.com/storage/docs/json_api/v1/buckets#lifecycle)
   *
   * <p>- The CLI syntax does not change when WSM API changes. In this case, the syntax affected is
   * the structure of the user-provided JSON file to specify a lifecycle rule.
   *
   * <p>- The CLI can more easily control the JSON mapping behavior of the enum. In this case, the
   * WSM client library version of the enum provides a @JsonCreator fromValue method that is case
   * sensitive, and the CLI may want to allow case insensitive deserialization.
   */
  public enum ActionType {
    Delete(GcpGcsBucketLifecycleRuleActionType.DELETE),
    SetStorageClass(GcpGcsBucketLifecycleRuleActionType.SET_STORAGE_CLASS);

    private GcpGcsBucketLifecycleRuleActionType wsmEnumVal;

    ActionType(GcpGcsBucketLifecycleRuleActionType wsmEnumVal) {
      this.wsmEnumVal = wsmEnumVal;
    }

    public GcpGcsBucketLifecycleRuleActionType toWSMEnum() {
      return this.wsmEnumVal;
    }
  }

  public static class Rule {
    public Action action;
    public Condition condition;
  }

  public static class Action {
    public ActionType type;
    public GcsStorageClass storageClass;
  }

  public static class Condition {
    public Integer age;
    public LocalDate createdBefore;
    public LocalDate customTimeBefore;
    public Integer daysSinceCustomTime;
    public Integer daysSinceNoncurrentTime;
    public Boolean isLive;
    public List<GcsStorageClass> matchesStorageClass = new ArrayList<>();
    public LocalDate noncurrentTimeBefore;
    public Integer numNewerVersions;
  }
}
