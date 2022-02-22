package bio.terra.cli.app.utils.tables;

public interface FieldTransformer<T> {
  // Extending classes need only provide an  array of QueryParameterColumns, such
  // as an enum class's values() array.
  TablePrintable<T> getColumns();
}

//public interface BigQueryInsertionPayloadTransformer<MODEL_T> {
//  // Extending classes need only provide an  array of QueryParameterColumns, such
//  // as an enum class's values() array.
//  ColumnValueExtractor<MODEL_T>[] getQueryParameterColumns();
//}
