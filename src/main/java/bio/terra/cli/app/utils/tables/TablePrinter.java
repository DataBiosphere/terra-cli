package bio.terra.cli.app.utils.tables;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Simple generic printer for object fields in rows separated by newlines and delimited by tabs.
 *
 * @param <T> User-facing type to be tabularized
 */
@FunctionalInterface
public interface TablePrinter<T> {

  String FIELD_DELIMITER = "\t";
  String ROW_DELIMITER = "\n";
  String EMPTY_FIELD_PLACEHOLDER = "(unset)";
  // To instantiate a TablePrinter, supply an array of `TablePrintable`s. If an Enum type implements
  // this array, the object may be instantiated simply by
  // TablePrinter<UFWorkspace> workspaceTablePrinter = UFWorkspaceColumns::values;

  PrintableColumn<T>[] getColumnEnumValues();

  /**
   * Print the list of objects to a table, with their column labels, ordering, and field values
   * determined by the enum array given by @Code {getColumnEnumValues}.
   *
   * @param rowObjects - list of user-facing objects to print to rows of the table.
   * @return table string
   */
  default String print(List<T> rowObjects) {
    String header = printHeaderRow();
    return header
        + ROW_DELIMITER
        + rowObjects.stream().map(this::printRow).collect(Collectors.joining(ROW_DELIMITER));
  }

  /** Fetch the column labels for each column and join them into a header row. */
  default String printHeaderRow() {
    return Arrays.stream(getColumnEnumValues())
        .map(PrintableColumn::getColumnLabel)
        .collect(Collectors.joining(FIELD_DELIMITER));
  }

  /**
   * Convert an object into a row based on its corresponding TablePrintable column objects.
   *
   * @param rowObject
   * @return
   */
  default String printRow(T rowObject) {
    return Arrays.stream(getColumnEnumValues())
        .map(
            r ->
                Optional.ofNullable(r.getValueExtractor().apply(rowObject))
                    .orElse(EMPTY_FIELD_PLACEHOLDER))
        .collect(Collectors.joining(FIELD_DELIMITER));
  }
}
