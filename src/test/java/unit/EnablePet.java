package unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.service.SamService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.TestIamPermissionsRequest;
import com.google.api.services.iam.v1.model.TestIamPermissionsResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for allowing a user to impersonate their pet SA in a workspace. */
@Tag("unit")
public class EnablePet extends SingleWorkspaceUnit {
  @Test
  @DisplayName("enable a user to impersonate their pet SA to run PAPI jobs")
  void enablePet() throws Exception {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // Before calling this action, user cannot impersonate the pet SA.
    String projectId = Context.getWorkspace().orElseThrow().getGoogleProjectId();
    String petEmail = SamService.fromContext().getPetSaEmailForProject(projectId);
    Iam userIamClient =
        iamClientFromGoogleCredentials(workspaceCreator.getCredentialsWithCloudPlatformScope());
    // TODO(PF-765): this will fail due to project-level SA permission.
    // assertFalse(canImpersonateSa(userIamClient, projectId, petEmail));

    TestCommand.runCommandExpectSuccess("workspace", "enable-pet");
    assertTrue(canImpersonateSa(userIamClient, projectId, petEmail));
  }

  private Iam iamClientFromGoogleCredentials(GoogleCredentials creds)
      throws GeneralSecurityException, IOException {
    return new Iam(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(),
        new HttpCredentialsAdapter(creds));
  }

  private boolean canImpersonateSa(Iam iamClient, String projectId, String petSaEmail)
      throws Exception {
    String fullyQualifiedSaName =
        String.format("projects/%s/serviceAccounts/%s", projectId, petSaEmail);
    TestIamPermissionsRequest testIamRequest =
        new TestIamPermissionsRequest()
            .setPermissions(Collections.singletonList("iam.serviceAccounts.actAs"));
    TestIamPermissionsResponse response =
        iamClient
            .projects()
            .serviceAccounts()
            .testIamPermissions(fullyQualifiedSaName, testIamRequest)
            .execute();
    // When no permissions are active, the permissions field of the response is null instead of an
    // empty list. This is a quirk of the GCP client library.
    return response.getPermissions() != null
        && response.getPermissions().contains("iam.serviceAccounts.actAs");
  }
}
