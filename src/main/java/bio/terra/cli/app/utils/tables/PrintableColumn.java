package bio.terra.cli.app.utils.tables;

import com.google.api.client.util.Strings;
import java.util.function.Function;

/**
 * Methods to allow table-formatting of a list of objects of type T
 *
 * @param <T> class of user-facing object
 */
public interface PrintableColumn<T> {
  String EMPTY_FIELD_PLACEHOLDER = "(unset)";

  enum Alignment {
    LEFT("-"),
    RIGHT("");

    private final String format;

    Alignment(String format) {
      this.format = format;
    }

    String getFormat() {
      return format;
    }
  }

  String getLabel();

  Function<T, String> getValueExtractor();

  int getWidth();

  Alignment getAlignment();

  default String formatLabel() {
    String format = "%" + Alignment.LEFT.getFormat() + getWidth() + "." + getWidth() + "s";
    return String.format(format, getLabel());
  }

  default String formatCell(T rowObject) {
    String rawText = getValueExtractor().apply(rowObject);
    String field = Strings.isNullOrEmpty(rawText) ? EMPTY_FIELD_PLACEHOLDER : rawText;
    String format = "%" + getAlignment().getFormat() + getWidth() + "." + getWidth() + "s";
    return String.format(format, field);
  }
}
