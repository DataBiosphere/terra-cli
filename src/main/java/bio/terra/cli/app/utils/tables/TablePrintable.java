package bio.terra.cli.app.utils.tables;

import java.util.function.Function;

/**
 * Methods to allow table-formatting of a list of objects of type T
 * @param <T> class of user-facing object
 */
@FunctionalInterface
public interface TablePrintable<T> extends FieldTransformer<T> {
  String getColumnLabel();
  Function<T, String> getValueExtractor();
}
