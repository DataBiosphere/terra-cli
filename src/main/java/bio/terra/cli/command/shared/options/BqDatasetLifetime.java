package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the default lifetime options for `terra resource` commands that
 * handle controlled Big Query Datasets.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class BqDatasetLifetime {

  @CommandLine.Option(
      names = "--default-partition-lifetime",
      description =
          "The default lifetime, in seconds, for partitions in newly created partitioned tables. "
              + "This flag has no minimum value. Specify 0 to remove the existing expiration "
              + "time. Any partitions in newly created partitioned tables are deleted this many "
              + "seconds after the partition's create time, adjusted to UTC "
              + "(@|underline,blue https://en.wikipedia.org/wiki/Coordinated_Universal_Time|@) "
              + "such that time zone changes will not affect this lifetime.  This value is "
              + "applied if you do not set a partition expiration on the table when it is "
              + "created.")
  private Integer defaultPartitionLifetimeSeconds;

  @CommandLine.Option(
      names = "--default-table-lifetime",
      description =
          "The default lifetime, in seconds, for newly created tables. The minimum value is "
              + "3600 seconds (one hour). Specify 0 to remove the existing expiration time. The "
              + "expiration time evaluates to the current time plus this value, adjusted to UTC "
              + "such that time zone changes will not affect this lifetime. Any table created in "
              + "the dataset is deleted this many seconds after its creation time. This value is "
              + "applied if you do not set a table expiration when the table is created.")
  private Integer defaultTableLifetimeSeconds;

  public boolean isDefined() {
    return (defaultPartitionLifetimeSeconds != null) || (defaultTableLifetimeSeconds != null);
  }

  public Integer getDefaultPartitionLifetimeSeconds() {
    return defaultPartitionLifetimeSeconds;
  }

  public Integer getDefaultTableLifetimeSeconds() {
    return defaultTableLifetimeSeconds;
  }
}
