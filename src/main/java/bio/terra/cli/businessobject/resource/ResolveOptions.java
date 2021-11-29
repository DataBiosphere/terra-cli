package bio.terra.cli.businessobject.resource;

/** This enum specifies the possible ways to resolve a BigQuery dataset resource. */
public enum ResolveOptions {
  FULL_PATH, // [project id].[dataset id].[data table id]
  TABLE_ID_ONLY, // [data table id]
  DATASET_ID_ONLY, // [dataset id]
  PROJECT_ID_ONLY; // [project id]
}
