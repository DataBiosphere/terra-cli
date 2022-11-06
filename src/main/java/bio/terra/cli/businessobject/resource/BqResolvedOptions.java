package bio.terra.cli.businessobject.resource;

/** This enum specifies the possible ways to resolve a BigQuery resource. */
public enum BqResolvedOptions {
  FULL_PATH, // For data table: [project id]:[dataset id].[data table id if applicable]
  FULL_PATH_SQL, // For data table: [project id].[dataset id].[data table id if applicable]
  TABLE_ID_ONLY, // [data table id]
  DATASET_ID_ONLY, // [dataset id]
  PROJECT_ID_ONLY; // [project id]

  /**
   * Delimiter between the project id and dataset id when using the command-line compatible
   * identifier format.
   */
  public static final char BQ_PROJECT_DELIMITER = ':';

  /**
   * Delimiter between the project id and dataset id when using the SQL-compatible identifier
   * format.
   */
  public static final char BQ_PROJECT_DELIMITER_SQL = '.';

  /** Delimiter between the dataset ID and data table. */
  public static final char BQ_TABLE_DELIMITER = '.';
}
