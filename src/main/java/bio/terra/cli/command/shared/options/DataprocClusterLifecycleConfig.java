package bio.terra.cli.command.shared.options;

import bio.terra.cli.exception.UserActionableException;
import java.time.OffsetDateTime;
import picocli.CommandLine;

/**
 * Command helper class that defines the Lifecycle configuration arg group. It adds the following
 * flags: --idle-delete-ttl, --auto-delete-ttl, auto-delete-time
 *
 * <p>This class is meant to be provided as an arg group to a {@WsmBaseCommand} class.
 */
public class DataprocClusterLifecycleConfig {
  @CommandLine.Option(
      names = "--idle-delete-ttl",
      description = "Time-to-live after which the resource becomes idle and is deleted.")
  public String idleDeleteTtl;

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  public AutoDeleteOptions autoDeleteOptions = new AutoDeleteOptions();

  public static class AutoDeleteOptions {
    @CommandLine.Option(
        names = "--auto-delete-ttl",
        description = "Time-to-live after which the resource is automatically deleted.")
    public String autoDeleteTtl;

    @CommandLine.Option(
        names = "--auto-delete-time",
        description = "Specific date and time after which the resource is automatically deleted.",
        converter = OffsetDateTimeConverter.class)
    public OffsetDateTime autoDeleteTime;
  }

  /** Helper class to convert a string to an {@link OffsetDateTime}. */
  private static class OffsetDateTimeConverter
      implements CommandLine.ITypeConverter<OffsetDateTime> {
    @Override
    public OffsetDateTime convert(String value) throws UserActionableException {
      return OffsetDateTime.parse(value);
    }
  }

  public boolean isDefined() {
    return idleDeleteTtl != null
        || autoDeleteOptions.autoDeleteTtl != null
        || autoDeleteOptions.autoDeleteTime != null;
  }
}
