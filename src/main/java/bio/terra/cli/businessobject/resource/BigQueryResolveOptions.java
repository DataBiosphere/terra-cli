package bio.terra.cli.businessobject.resource;

/** This enum specifies the possible ways to resolve a BigQuery dataset resource. */
public enum BigQueryResolveOptions {
  FULL_PATH, // For data table: [project id].[dataset id].[data table id if applicable]
  TABLE_ID_ONLY, // [data table id]
  DATASET_ID_ONLY, // [dataset id]
  PROJECT_ID_ONLY; // [project id]

  /**
   * Delimiter between the project id, dataset id and data table id for a BigQuery DataTable.
   *
   * <p>The choice is somewhat arbitrary. BigQuery tables do not have true URIs. The '.' delimiter
   * allows the path to be used directly in SQL calls with a BigQuery extension.
   */
  public static final char BQ_PROJECT_DATA_TABLE_DELIMITER = '.';
}
