package bio.terra.cli.apps.interfaces;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;

/** Classes that implement this interface can be enabled with the 'terra app enable __' command. */
public interface Supported {
  /** Set the global and workspace context properties. */
  void setContext(GlobalContext globalContext, WorkspaceContext workspaceContext);

  /**
   * This enum specifies the list returned by the 'terra app list' command. Classes that implement
   * this interface should also be added to this enum.
   */
  public enum SupportedApp {
    nextflow,
    gcloud,
    gsutil,
    bq
  }
}
