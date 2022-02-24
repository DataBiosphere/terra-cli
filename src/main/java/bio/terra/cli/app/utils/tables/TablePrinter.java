package bio.terra.cli.app.utils.tables;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Simple generic printer for object fields in rows separated by newlines and delimited by tabs.
 *
 * @param <T> User-facing type to be tabularized
 */
@FunctionalInterface
public interface TablePrinter<T> {

  String FIELD_DELIMITER = "\t";
  String ROW_DELIMITER = "\n";
  // To instantiate a TablePrinter, supply an array of `TablePrintable`s. If an Enum type implements
  // this array, the object may be instantiated simply by
  // TablePrinter<UFWorkspace> workspaceTablePrinter = UFWorkspaceColumns::values;

  PrintableColumn<T>[] getColumnEnumValues();

  default String print(List<T> rowObjects) {
    return print(rowObjects, null);
  }

  /**
   * Print the list of objects to a table, with their column labels, ordering, and field values
   * determined by the enum array given by @Code {getColumnEnumValues}.
   *
   * @param rowObjects - list of user-facing objects to print to rows of the table.
   * @param isHighlighted - boolean-valued function to tell if a row should be highlighted (starred)
   * @return table string
   */
  default String print(List<T> rowObjects, @Nullable Predicate<T> isHighlighted) {
    boolean includeHighlightColumn = null != isHighlighted;
    String header = (includeHighlightColumn ? "   " : "") + printHeaderRow();
    return header
        + ROW_DELIMITER
        + rowObjects.stream()
            .map(r -> printRow(r, isHighlighted))
            .collect(Collectors.joining(ROW_DELIMITER));
  }

  /** Fetch the column labels for each column and join them into a header row. */
  default String printHeaderRow() {
    return Arrays.stream(getColumnEnumValues())
        .map(PrintableColumn::formatLabel)
        .collect(Collectors.joining(FIELD_DELIMITER));
  }

  /**
   * Convert an object into a row based on its corresponding TablePrintable column objects.
   *
   * @param rowObject - object corresponding to this row
   * @param isHighlighted - boolean-valued function to tell if a row should be highlighted
   *     (starred). null if this table type does not highlight any rows
   * @return
   */
  default String printRow(T rowObject, @Nullable Predicate<T> isHighlighted) {
    final String highlightColumn;
    if (null == isHighlighted) {
      highlightColumn = "";
    } else if (isHighlighted.test(rowObject)) {
      highlightColumn = " * ";
    } else {
      highlightColumn = "   ";
    }

    return highlightColumn
        + Arrays.stream(getColumnEnumValues())
            .map(c -> c.formatCell(rowObject))
            .collect(Collectors.joining(FIELD_DELIMITER));
  }
}
