package bio.terra.cli.command.shared.options;

import java.util.Optional;
import picocli.CommandLine;

/**
 * Command helper class that defines the expiration options for `terra resource` commands that
 * handle Big Query Datasets.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class BqDatasetExpirationOptions {
  @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..1")
  public ExpirationArgGroup expirationArgGroup;

  static class ExpirationArgGroup {
    @CommandLine.Option(
        names = "--partition-expiration",
        description =
            "Number of seconds after which to auto-delete newly created partitions in dataset")
    private Integer partitionExpiration;

    @CommandLine.Option(
        names = "--table-expiration",
        description =
            "Number of seconds after which to auto-delete newly created tables in dataset")
    private Integer tableExpiration;
  }

  public boolean isDefined() {
    return expirationArgGroup != null;
  }

  public Optional<Integer> getPartitionExpiration() {
    if (expirationArgGroup == null) return Optional.empty();

    return Optional.ofNullable(expirationArgGroup.partitionExpiration);
  }

  public Optional<Integer> getTableExpiration() {
    if (expirationArgGroup == null) return Optional.empty();

    return Optional.ofNullable(expirationArgGroup.tableExpiration);
  }
}
