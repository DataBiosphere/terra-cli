package bio.terra.cli.app.utils.tables;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import java.util.function.Function;

public enum UFWorkspaceColumns implements TablePrintable<UFWorkspace> {
  NAME("NAME", w -> w.name),
  DESCRIPTION("DESCRIPTION", w -> w.description),
  GOOGLE_PROJECT("GOOGLE PROJECT", w -> w.googleProjectId),
  ID("ID", w -> w.id.toString());

  private final String columnLabel;
  private final Function<UFWorkspace, String> valueExtractor;

  UFWorkspaceColumns(String columnLabel, Function<UFWorkspace, String> valueExtractor) {
    this.columnLabel = columnLabel;
    this.valueExtractor = valueExtractor;
  }

  @Override
  public String getColumnLabel() {
    return columnLabel;
  }

  @Override
  public Function<UFWorkspace, String> getValueExtractor() {
    return valueExtractor;
  }
}
