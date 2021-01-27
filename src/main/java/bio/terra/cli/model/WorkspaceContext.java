package bio.terra.cli.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This POJO class represents an instance of the Terra CLI workspace context. This is intended
 * primarily for project and resource-related context values that will be particular to a single
 * workspace.
 */
public class WorkspaceContext {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceContext.class);

  public String terraWorkspaceId;
  public String googleProjectId;

  private static final Path WORKSPACE_CONTEXT_DIR = Paths.get("", ".terra-cli");
  private static final String WORKSPACE_CONTEXT_FILENAME = "workspace_context.json";

  private WorkspaceContext() {
    this.terraWorkspaceId = null;
    this.googleProjectId = null;
  }
}
