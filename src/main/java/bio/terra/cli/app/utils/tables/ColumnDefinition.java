package bio.terra.cli.app.utils.tables;

import com.google.api.client.util.Strings;
import java.util.function.Function;

/**
 * Methods to allow table-formatting of a list of objects of type T
 *
 * @param <T> class of user-facing object
 */
public interface ColumnDefinition<T> {
  String EMPTY_FIELD_PLACEHOLDER = "(unset)";

  enum Alignment {
    LEFT("-"),
    RIGHT("");

    private final String format;

    Alignment(String format) {
      this.format = format;
    }

    /**
     * Return the prefix for use in a format string such as {@code %-20.20s} for left alignment, or
     * nothing for default (right) alignment.
     */
    String getFormat() {
      return format;
    }
  }

  /** Get the column header label, before any formatting. */
  String getLabel();

  /** Get a method to pull a string value for this column out of the object of type T */
  Function<T, String> getValueExtractor();

  /** Get column width, in characters. */
  int getWidth();

  /** Get column horizontal alignment. */
  Alignment getAlignment();

  /** Apply formatting to the column label. */
  default String formatLabel() {
    String format = "%" + Alignment.LEFT.getFormat() + getWidth() + "." + getWidth() + "s";
    return String.format(format, getLabel());
  }

  /** Apply padding and alignment to a cell's string value. */
  default String formatCell(T rowObject) {
    String rawText = getValueExtractor().apply(rowObject);
    String field = Strings.isNullOrEmpty(rawText) ? EMPTY_FIELD_PLACEHOLDER : rawText;
    String format = "%" + getAlignment().getFormat() + getWidth() + "." + getWidth() + "s";
    return String.format(format, field);
  }
}
