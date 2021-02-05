package bio.terra.cli.app.supported;

import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;

/** This enum lists all the supported apps, and whether they can be enabled and stopped. */
public enum SupportedApp {
  bq(),
  gcloud(),
  gsutil(),
  nextflow(new NextflowHelper());

  private SupportedAppHelper toolHelper;
  public final boolean enableStopPattern;

  SupportedApp() {
    this.enableStopPattern = false;
    this.toolHelper = null;
  }

  SupportedApp(SupportedAppHelper toolHelper) {
    this.enableStopPattern = true;
    this.toolHelper = toolHelper;
  }

  public SupportedAppHelper getToolHelper(
      GlobalContext globalContext, WorkspaceContext workspaceContext) {
    return toolHelper.setContext(globalContext, workspaceContext);
  }
}
