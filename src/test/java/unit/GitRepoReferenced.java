package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class GitRepoReferenced extends SingleWorkspaceUnit {

  private static final String GIT_REPO_SSH_URL =
      "git@github.com:DataBiosphere/terra-workspace-manager.git";
  private static final String GIT_REPO_HTTPS_URL =
      "https://github.com/DataBiosphere/terra-workspace-manager.git";

  @Test
  @DisplayName("list and describe reflect adding a new referenced git repo")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref git-repo --name=$name --repo-url=$repoUrl
    String name = "listDescribeReflectAdd";
    UFGitRepo addedGitRepositoryReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "add-ref",
            "git-repo",
            "--name=" + name,
            "--repo-url=" + GIT_REPO_HTTPS_URL);

    // check that the name and git repo name match
    assertEquals(name, addedGitRepositoryReference.name, "add ref output matches name");
    assertEquals(
        GIT_REPO_HTTPS_URL,
        addedGitRepositoryReference.gitRepoUrl,
        "add ref output matches git repo url");

    TestCommand.runCommand("status");
    // `terra resource describe --name=$name --format=json`
    UFGitRepo describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class, "resource", "describe", "--name=" + name);

    // check that the url and git repo url match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        GIT_REPO_HTTPS_URL,
        describeResource.gitRepoUrl,
        "describe resource output matches git repo url");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }
}
