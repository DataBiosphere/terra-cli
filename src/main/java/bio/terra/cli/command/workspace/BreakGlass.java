package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.WorkspaceUser;
import bio.terra.cli.cloud.auth.GoogleOauth;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.GcpContext;
import bio.terra.workspace.model.WorkspaceDescription;
import com.google.api.client.util.DateTime;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace break-glass" command. */
@Command(name = "break-glass", description = "Grant break-glass access to a workspace user.")
public class BreakGlass extends WsmBaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(BreakGlass.class);
  // pointers to the central BQ dataset where break-glass requests are logged
  // keep the dataset/table names here consistent with those in tools/create-break-glass-bq.sh
  private static final String BQ_DATASET_NAME = "break_glass_requests";
  private static final String BQ_TABLE_NAME = "requests";

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Option(
      names = "--email",
      required = true,
      description = "Email of workspace user requesting break-glass access.")
  private String granteeEmail;

  @CommandLine.Option(
      names = "--user-project-admin-sa",
      required = true,
      description =
          "Path to the key file for a SA that has permission to set IAM policy on workspace projects for the current server.")
  private String userProjectAdminSAKeyFile;

  @CommandLine.Option(
      names = "--big-query-sa",
      required = true,
      description =
          "Path to the key file for a SA that has permissions to update the BigQuery dataset tracking break-glass requests.")
  private String bigQuerySAKeyFile;

  @CommandLine.Option(
      names = "--big-query-project",
      required = true,
      description = "Project ID that contains the BigQuery dataset tracking break-glass requests.")
  private String bigQueryProjectId;

  @CommandLine.Option(
      names = "--notes",
      description = "Free text string about this request, to store in the BigQuery dataset.")
  private String notes;

  /** Grant break-glass access to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // check that the SA key files exist and are valid
    ServiceAccountCredentials userProjectsAdminCredentials;
    ServiceAccountCredentials bigQueryCredentials;
    try {
      final List<String> SA_SCOPES =
          ImmutableList.of("https://www.googleapis.com/auth/cloud-platform");
      userProjectsAdminCredentials =
          GoogleOauth.getServiceAccountCredential(
              Path.of(userProjectAdminSAKeyFile).toFile(), SA_SCOPES);
      bigQueryCredentials =
          GoogleOauth.getServiceAccountCredential(Path.of(bigQuerySAKeyFile).toFile(), SA_SCOPES);
    } catch (IOException ioEx) {
      throw new UserActionableException("Error reading break-glass SA key files.", ioEx);
    }

    // require that the requester is a workspace owner
    Workspace currentWorkspace = Context.requireWorkspace();
    Optional<WorkspaceUser> granteeWorkspaceUser =
        WorkspaceUser.list(currentWorkspace).stream()
            .filter(user -> user.getEmail().equalsIgnoreCase(granteeEmail))
            .findAny();
    if (granteeWorkspaceUser.isEmpty()
        || !granteeWorkspaceUser.get().getRoles().contains(WorkspaceUser.Role.OWNER)) {
      updateRequestsCatalogWithFailure(bigQueryCredentials, "Requestor is not a workspace owner.");
      throw new UserActionableException(
          "The break-glass requester must be an owner of the workspace.");
    }

    // grant the user's proxy group the Editor role on the workspace project
    String granteeProxyGroupEmail =
        currentWorkspace.grantBreakGlass(granteeEmail, userProjectsAdminCredentials);

    // update the central BigQuery dataset with details of this request
    updateRequestsCatalogWithSuccess(bigQueryCredentials, granteeProxyGroupEmail);

    OUT.println("Break-glass access successfully granted to: " + granteeEmail);
  }

  private void updateRequestsCatalogWithFailure(
      ServiceAccountCredentials bigQueryCredentials, String errorMsg) {
    updateRequestsCatalog(bigQueryCredentials, true, null, errorMsg);
  }

  private void updateRequestsCatalogWithSuccess(
      ServiceAccountCredentials bigQueryCredentials, String granteeProxyGroupEmail) {
    updateRequestsCatalog(bigQueryCredentials, false, granteeProxyGroupEmail, null);
  }

  private void updateRequestsCatalog(
      ServiceAccountCredentials bigQueryCredentials,
      boolean isFailure,
      String granteeProxyGroupEmail,
      String errorMsg) {
    BigQuery bigQueryClient =
        BigQueryOptions.newBuilder()
            .setProjectId(bigQueryProjectId)
            .setCredentials(bigQueryCredentials)
            .build()
            .getService();
    Map<String, Object> rowContent = new HashMap<>();
    rowContent.put("id", UUID.randomUUID().toString());
    rowContent.put("requestTimestamp", new DateTime(new Date()));
    rowContent.put("isFailure", isFailure);
    rowContent.put("errorMsg", errorMsg);
    rowContent.put("granteeEmail", granteeEmail);
    rowContent.put("granteeProxyGroupEmail", granteeProxyGroupEmail);
    rowContent.put("serverName", Context.getServer().getName());
    rowContent.put("serverWsmUri", Context.getServer().getWorkspaceManagerUri());
    rowContent.put("workspaceId", Context.requireWorkspace().getUuid().toString());
    WorkspaceDescription workspaceDescription =
        Context.requireWorkspace().getWorkspaceDescription();
    GcpContext gcpContext = workspaceDescription.getGcpContext();
    if (gcpContext != null) {
      rowContent.put("googleProjectId", gcpContext.getProjectId());
    }
    rowContent.put("workspaceName", workspaceDescription.getDisplayName());
    rowContent.put("workspaceDescription", workspaceDescription.getDescription());
    rowContent.put("granterEmail", Context.requireUser().getEmail());
    rowContent.put("requestNotes", notes);

    TableId tableId = TableId.of(BQ_DATASET_NAME, BQ_TABLE_NAME);
    InsertAllRequest insertRequest =
        InsertAllRequest.newBuilder(tableId).addRow(rowContent).build();
    InsertAllResponse insertResponse = bigQueryClient.insertAll(insertRequest);
    if (insertResponse.hasErrors()) {
      logger.error(
          "hasErrors is true after inserting into the {}.{}.{} table",
          bigQueryClient.getOptions().getProjectId(),
          insertRequest.getTable().getDataset(),
          insertRequest.getTable().getTable());
      // log any insertion errors
      for (Map.Entry<Long, List<BigQueryError>> entry :
          insertResponse.getInsertErrors().entrySet()) {
        entry
            .getValue()
            .forEach(
                bqErr ->
                    logger.error(
                        "Error inserting row into break glass BigQuery table: {}",
                        bqErr.toString()));
      }
      if (insertResponse.getInsertErrors().entrySet().size() > 0) {
        throw new SystemException(
            "Error updating BigQuery dataset that catalogs break-glass requests.");
      }
    }
  }
}
