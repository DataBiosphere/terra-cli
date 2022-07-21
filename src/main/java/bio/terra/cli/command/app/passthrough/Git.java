package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.DataCollection;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "git", description = "Call git in the Terra workspace.")
public class Git extends ToolCommand {
  private static final Logger logger = LoggerFactory.getLogger(Git.class);

  @CommandLine.Option(
      names = "--resource",
      split = ",",
      description =
          "name of the git-repo resources in the current workspace to be cloned. \n"
              + "Example usage: git clone --resource=repo1,repo2")
  public String[] names;

  @CommandLine.Option(
      names = "--all",
      description = "clone all the git repo resources in the current workspace")
  public boolean cloneAll;

  @Override
  public String getExecutableName() {
    return "git";
  }

  @Override
  public String getInstallationUrl() {
    return "https://git-scm.com/book/en/v2/Getting-Started-Installing-Git";
  }

  @Override
  protected void executeImpl() {
    workspaceOption.overrideIfSpecified();
    command.add(0, "git");
    if (cloneAll && names != null && names.length > 0) {
      throw new UserActionableException(
          "Conflicted input argument. Only specify one depending on "
              + "whether you'd like to clone all or some of the git repo resources in this workspace");
    }
    if (cloneAll || (names != null && names.length > 0)) {
      validateCloneCommand();

      List<Resource> gitReposToClone = new ArrayList<>();
      if (cloneAll) {
        var resources = Context.requireWorkspace().getResources();
        gitReposToClone.addAll(getGitReposToClone(resources));
        resources.stream()
            .filter(resource -> Resource.Type.DATA_COLLECTION == resource.getResourceType())
            .forEach(
                resource ->
                    gitReposToClone.addAll(
                        attemptToGetGitReposInDataCollectionToClone((DataCollection) resource)));
      } else {
        for (String name : names) {
          var resource = Context.requireWorkspace().getResource(name);
          if (Resource.Type.GIT_REPO != resource.getResourceType()) {
            throw new UserActionableException(
                String.format(
                    "%s %s cannot be cloned because it is not a git repo resource",
                    resource.getResourceType(), resource.getName()));
          }
          gitReposToClone.add(resource);
        }
      }
      cloneAll(gitReposToClone);
    }

    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }

  private void validateCloneCommand() {
    if (command.size() != 2 || !command.get(1).equals("clone")) {
      throw new UserActionableException(
          "Did you mean to clone git repo resources in the workspace? If so, please use terra git clone");
    }
  }

  private List<Resource> attemptToGetGitReposInDataCollectionToClone(
      DataCollection dataCollection) {
    Workspace dataCollectionWorkspace = null;
    try {
      dataCollectionWorkspace = dataCollection.getDataCollectionWorkspace();
    } catch (SystemException e) {
      // If a user does not have access to the data collection, do not throw.
      logger.warn(String.format("Failed to get Data collection %s", dataCollection.getName()));
    }
    if (dataCollectionWorkspace != null) {
      return getGitReposToClone(dataCollectionWorkspace.getResources());
    }
    return Collections.emptyList();
  }

  private List<Resource> getGitReposToClone(List<Resource> resources) {
    return resources.stream()
        .filter(resource -> Resource.Type.GIT_REPO == resource.getResourceType())
        .collect(Collectors.toList());
  }

  private void cloneAll(List<Resource> resources) {
    for (var resource : resources) {
      Preconditions.checkArgument(Resource.Type.GIT_REPO == resource.getResourceType());
      List<String> cloneCommands = Stream.of("git", "clone").collect(Collectors.toList());
      String gitRepoUrl = resource.resolve();
      cloneCommands.add(gitRepoUrl);
      try {
        Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(cloneCommands);
      } catch (PassthroughException e) {
        ERR.println("Git clone for " + gitRepoUrl + " failed");
      }
    }
  }
}
