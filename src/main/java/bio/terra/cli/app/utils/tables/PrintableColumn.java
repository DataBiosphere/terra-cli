package bio.terra.cli.app.utils.tables;

import java.util.Optional;
import java.util.function.Function;

/**
 * Methods to allow table-formatting of a list of objects of type T
 *
 * @param <T> class of user-facing object
 */
public interface PrintableColumn<T> {
  String EMPTY_FIELD_PLACEHOLDER = "(unset)";

  String getLabel();

  Function<T, String> getValueExtractor();

  int getWidth();

  default String formatLabel() {
    String format = "%" + getWidth() + "." + getWidth() + "s";
    return String.format(format, getLabel());
  }

  default String formatCell(T rowObject) {
    String field =
        Optional.ofNullable(getValueExtractor().apply(rowObject)).orElse(EMPTY_FIELD_PLACEHOLDER);
    String format = "%" + getWidth() + "." + getWidth() + "s";
    return String.format(format, field);
  }
}
