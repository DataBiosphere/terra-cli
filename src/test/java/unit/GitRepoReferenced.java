package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

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

    // check that the git repo is in the list
    List<UFGitRepo> matchedResourceList = listGitRepoResourcesWithName(name);
    assertEquals(1, matchedResourceList.size());
    UFGitRepo matchedResource = matchedResourceList.get(0);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        GIT_REPO_HTTPS_URL, matchedResource.gitRepoUrl, "list output matches git repo url");

    // `terra resource describe --name=$name --format=json`
    UFGitRepo describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class, "resource", "describe", "--name=" + name);

    // check that the name and the git repo url match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        GIT_REPO_HTTPS_URL,
        describeResource.gitRepoUrl,
        "describe resource output matches git repo url");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("resolve a referenced git repo")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref git-repo --name=$name --repo-url=$repoUrl
    String name = "resolve";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGitRepo.class,
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + name,
        "--repo-url=" + GIT_REPO_SSH_URL);

    // `terra resource resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name);
    assertEquals(GIT_REPO_SSH_URL, resolved, "resolve matches git repo ssh url");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a referenced object")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref git-repo --name=$name --repo-url=$repoUrl
    String name = "listReflectsDelete";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGitRepo.class,
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + name,
        "--repo-url=" + GIT_REPO_SSH_URL);

    // `terra resource delete --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // check that the object is not in the list
    List<UFGitRepo> matchedResources = listGitRepoResourcesWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("add a referenced git repo, specifying all options")
  void addWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources add-ref git-repo --name=$name --repo-url=$repoUrl
    // --cloning=$cloning --description=$description --format=json`
    String name = "addWithAllOptions";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.REFERENCE;
    String description = "add with all options";
    UFGitRepo gitRepoReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "add-ref",
            "git-repo",
            "--name=" + name,
            "--description=" + description,
            "--cloning=" + cloning,
            "--repo-url=" + GIT_REPO_SSH_URL);

    // check that the properties match
    assertEquals(name, gitRepoReference.name, "add ref output matches name");
    assertEquals(
        GIT_REPO_SSH_URL, gitRepoReference.gitRepoUrl, "add ref output matches git repo url");
    assertEquals(cloning, gitRepoReference.cloningInstructions, "add ref output matches cloning");
    assertEquals(description, gitRepoReference.description, "add ref output matches description");

    // `terra resources describe --name=$name --format=json`
    UFGitRepo describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class, "resource", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        GIT_REPO_SSH_URL,
        describeResource.gitRepoUrl,
        "describe resource output matches git repo url");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("update a referenced git repo, one property at a time")
  void updateIndividualProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref git-repo --name=$name --repo-url=$repoUrl --description=$description`
    String name = "updateIndividualProperties";
    String description = "updateDescription";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGitRepo.class,
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + name,
        "--description=" + description,
        "--repo-url=" + GIT_REPO_SSH_URL);

    // update just the name
    // `terra resources update git-repo --name=$name --new-name=$newName`
    String newName = "updateIndividualProperties_NEW";
    UFGitRepo updatedGitRepo =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "update",
            "git-repo",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updatedGitRepo.name);
    assertEquals(description, updatedGitRepo.description);

    // `terra resources describe --name=$newName`
    UFGitRepo describedGitRepo =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class, "resource", "describe", "--name=" + newName);
    assertEquals(description, describedGitRepo.description);
    assertEquals(CloningInstructionsEnum.REFERENCE, describedGitRepo.cloningInstructions);

    // update description and cloning instructions
    // `terra resources update git-repo --name=$newName --new-description=$newDescription
    // --new-cloning=$CloningInstructionsEnum.NOTHING`
    String newDescription = "updateDescription_NEW";
    updatedGitRepo =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "update",
            "git-repo",
            "--name=" + newName,
            "--new-description=" + newDescription,
            "--new-cloning=" + CloningInstructionsEnum.NOTHING);
    assertEquals(newName, updatedGitRepo.name);
    assertEquals(newDescription, updatedGitRepo.description);

    // `terra resources describe --name=$newName`
    describedGitRepo =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describedGitRepo.description);
    assertEquals(CloningInstructionsEnum.NOTHING, describedGitRepo.cloningInstructions);

    updatedGitRepo =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "update",
            "git-repo",
            "--name=" + newName,
            "--new-repo-url=" + GIT_REPO_HTTPS_URL);
    assertEquals(GIT_REPO_HTTPS_URL, updatedGitRepo.gitRepoUrl);

    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + newName);
    assertEquals(GIT_REPO_HTTPS_URL, resolved, "resolve matches https Git url");
  }

  @Test
  @DisplayName("update a referenced git repo, specifying multiple or none of the properties")
  void updateMultipleOrNoProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources add-ref git-repo --name=$name --description=$description
    // --repo-url=$gitUrl
    String name = "updateMultipleOrNoProperties";
    String description = "updateDescription";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGitRepo.class,
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + name,
        "--description=" + description,
        "--repo-url=" + GIT_REPO_SSH_URL);

    // call update without specifying any properties to modify
    // `terra resources update git-repo --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "update", "git-repo", "--name=" + name);
    assertThat(
        "Specify at least one property to update..",
        stdErr,
        CoreMatchers.containsString("Specify at least one property to update."));

    // update the name and description
    // `terra resources update gcs-object --name=$name --new-name=$newName
    // --new-description=$newDescription`
    String newName = "updateMultipleOrNoProperties_NEW";
    String newDescription = "updateDescription_NEW";
    UFGitRepo updateGitRepoNameAndDescription =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "update",
            "git-repo",
            "--name=" + name,
            "--new-name=" + newName,
            "--new-description=" + newDescription);
    assertEquals(newName, updateGitRepoNameAndDescription.name);
    assertEquals(newDescription, updateGitRepoNameAndDescription.description);

    // `terra resources describe --name=$newName2`
    UFGitRepo describeGitRepo =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeGitRepo.description);

    // update referencing target
    // `terra resources update git-repo --name=$name --new-repo-url=$newRepoUrl
    // --new-name=$newName --new-description=$newDescription
    String yetAnotherName = "updateMultipleOrNoProperties_NEW";
    String yetAnotherDescription = "updateDescription_NEW";
    UFGitRepo updateGitRepoReferenceTarget =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "update",
            "git-repo",
            "--name=" + newName,
            "--new-name=" + yetAnotherName,
            "--new-description=" + yetAnotherDescription,
            "--new-repo-url=" + GIT_REPO_HTTPS_URL);
    assertEquals(GIT_REPO_HTTPS_URL, updateGitRepoReferenceTarget.gitRepoUrl);

    UFGitRepo describeGitRepoAfterUpdatingReferenceTarget =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class, "resource", "describe", "--name=" + yetAnotherName);
    assertEquals(yetAnotherDescription, describeGitRepoAfterUpdatingReferenceTarget.description);
    assertEquals(GIT_REPO_HTTPS_URL, describeGitRepoAfterUpdatingReferenceTarget.gitRepoUrl);
    assertEquals(yetAnotherName, describeGitRepoAfterUpdatingReferenceTarget.name);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  static List<UFGitRepo> listGitRepoResourcesWithName(String resourceName)
      throws JsonProcessingException {
    // `terra resources list --type=GIT_REPO --format=json`
    List<UFGitRepo> listedResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=GIT_REPO");

    // find the matching git repo in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
