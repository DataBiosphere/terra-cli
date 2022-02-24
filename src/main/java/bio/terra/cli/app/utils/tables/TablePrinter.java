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

  /** Whitespace between columns. */
  String FIELD_DELIMITER = "  ";

  /** Separation between rows. */
  String ROW_DELIMITER = "\n";

  /**
   * Functional interface caller-supplied method. Returns an array of PrintableColumns. To
   * instantiate a TablePrinter, supply an array of `TablePrintable`s. If an Enum type implements
   * this array, the object may be instantiated simply by {@code TablePrinter<UFWorkspace>
   * workspaceTablePrinter = UFWorkspaceColumns::values;}. This creates an instance of an anonymous
   * class implementing TablePrinter.
   */
  ColumnDefinition<T>[] getColumnEnumValues();

  /**
   * Print a table from a list of row objects of type T. Do not leave space for a highlight column.
   *
   * @param rowObjects - list of user-facing objects to print to rows of the table.
   * @return string representation of table suitable for printing to console
   */
  default String print(List<T> rowObjects) {
    return print(rowObjects, null);
  }

  /**
   * Print the list of objects to a table, with their column labels, ordering, and field values
   * determined by the enum array given by @Code {getColumnEnumValues}.
   *
   * @param rowObjects - list of user-facing objects to print to rows of the table.
   * @param isHighlighted - boolean-valued function to tell if a row should be highlighted (starred)
   * @return table string suitable for printing to console
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
  private String printHeaderRow() {
    return Arrays.stream(getColumnEnumValues())
        .map(ColumnDefinition::formatLabel)
        .collect(Collectors.joining(FIELD_DELIMITER));
  }

  /**
   * Convert an object into a row based on its corresponding TablePrintable column objects.
   *
   * @param rowObject - object corresponding to this row
   * @param isHighlighted - boolean-valued function to tell if a row should be highlighted
   *     (starred). null if this table type does not highlight any rows
   * @return string for single row of the table
   */
  private String printRow(T rowObject, @Nullable Predicate<T> isHighlighted) {
    final String highlightColumn;
    if (null == isHighlighted) {
      highlightColumn = "";
    } else if (isHighlighted.test(rowObject)) {
      highlightColumn = " ✓ ";
    } else {
      highlightColumn = "   ";
    }

    return highlightColumn
        + Arrays.stream(getColumnEnumValues())
            .map(c -> c.formatCell(rowObject))
            .collect(Collectors.joining(FIELD_DELIMITER));
  }
}
