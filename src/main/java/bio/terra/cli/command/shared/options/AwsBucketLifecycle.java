package bio.terra.cli.command.shared.options;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.JacksonMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import picocli.CommandLine;

/**
 * Command helper class that defines the --lifecycle option for `terra resource` commands that
 * handle AWS bucket controlled resources.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class AwsBucketLifecycle {
  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  public LifecycleArgGroup lifecycleArgGroup;

  /** Helper method to build the lifecycle object . */
  public bio.terra.cli.serialization.userfacing.input.AwsBucketLifecycle buildLifecycleObject() {
    if (lifecycleArgGroup == null) {
      // empty lifecycle rule object
      return new bio.terra.cli.serialization.userfacing.input.AwsBucketLifecycle();
    } else if (lifecycleArgGroup.autoDelete != null) {
      // build an auto-delete lifecycle rule and set the number of days
      return bio.terra.cli.serialization.userfacing.input.AwsBucketLifecycle.buildAutoDeleteRule(
          lifecycleArgGroup.autoDelete);
    } else {
      // read in the lifecycle rules from a file
      try {
        return JacksonMapper.readFileIntoJavaObject(
            lifecycleArgGroup.pathToLifecycleFile.toFile(),
            bio.terra.cli.serialization.userfacing.input.AwsBucketLifecycle.class,
            Collections.singletonList(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
      } catch (IOException ioEx) {
        throw new UserActionableException("Error reading lifecycle rules from file.", ioEx);
      }
    }
  }

  public boolean isDefined() {
    return lifecycleArgGroup != null;
  }

  static class LifecycleArgGroup {
    @CommandLine.Option(
        names = "--lifecycle",
        description =
            "Lifecycle rules (https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html) specified in a JSON-formatted file. See the README for the expected JSON format.")
    private Path pathToLifecycleFile;

    @CommandLine.Option(
        names = "--auto-delete",
        description =
            "Number of days after which to auto-delete the objects in the bucket. This option is a shortcut for specifying a lifecycle rule that auto-deletes objects in the bucket after some number of days.")
    private Integer autoDelete;
  }
}
