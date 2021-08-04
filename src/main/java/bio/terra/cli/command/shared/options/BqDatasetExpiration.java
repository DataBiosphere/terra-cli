package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the expiration options for `terra resource` commands that
 * handle Big Query Datasets.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class BqDatasetExpiration {

  @CommandLine.Option(
      names = "--default-partition-expiration",
      description =
          "The default lifetime, in seconds, for partitions in newly created partitioned tables. This flag has no minimum value. Specify 0 to remove the existing expiration time. Any partitions in newly created partitioned tables are deleted this many seconds after the partition's UTC date. This value is applied if you do not set a partition expiration on the table when it is created.")
  private Integer partitionExpiration;

  @CommandLine.Option(
      names = "--default-table-expiration",
      description =
          "The default lifetime, in seconds, for newly created tables. The minimum value is 3600 seconds (one hour). The expiration time evaluates to the current UTC time plus this value. Specify 0 to remove the existing expiration time. Any table created in the dataset is deleted this many seconds after its creation time. This value is applied if you do not set a table expiration when the table is created.")
  private Integer tableExpiration;

  public boolean isDefined() {
    return (partitionExpiration != null) || (tableExpiration != null);
  }

  public Integer getPartitionExpiration() {
    return partitionExpiration;
  }

  public Integer getTableExpiration() {
    return tableExpiration;
  }
}
