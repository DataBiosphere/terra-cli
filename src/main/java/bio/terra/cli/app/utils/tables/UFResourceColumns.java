package bio.terra.cli.app.utils.tables;

import bio.terra.cli.serialization.userfacing.UFResource;
import java.util.function.Function;

public enum UFResourceColumns implements PrintableColumn<UFResource> {
  NAME("NAME", r -> r.name),
  RESOURCE_TYPE("RESOURCE TYPE", r -> r.resourceType.toString()),
  STEWARDSHIP_TYPE("STEWARDSHIP TYPE", r -> r.stewardshipType.toString()),
  DESCRIPTION("DESCRIPTION", r -> r.description);

  private final String columnLabel;
  private final Function<UFResource, String> valueExtractor;

  UFResourceColumns(String columnLabel, Function<UFResource, String> valueExtractor) {
    this.columnLabel = columnLabel;
    this.valueExtractor = valueExtractor;
  }

  @Override
  public String getColumnLabel() {
    return columnLabel;
  }

  @Override
  public Function<UFResource, String> getValueExtractor() {
    return valueExtractor;
  }
}
