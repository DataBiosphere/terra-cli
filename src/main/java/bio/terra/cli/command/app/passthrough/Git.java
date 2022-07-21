package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.DataCollection;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
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
    if (cloneAll) {
      validateCloneCommand();
      var resources = Context.requireWorkspace().getResources();
      getGitRepoResourceToClone(resources);
      resources.stream()
          .filter(resource -> Resource.Type.DATA_COLLECTION == resource.getResourceType())
          .forEach(
              resource -> {
                Workspace dataCollectionWorkspace =
                    attemptToGetDataCollectionWorkspace((DataCollection) resource);
                if (dataCollectionWorkspace != null) {
                  getGitRepoResourceToClone(dataCollectionWorkspace.getResources());
                }
              });
      return;
    }
    if (names != null) {
      validateCloneCommand();
      for (String name : names) {
        var resource = Context.requireWorkspace().getResource(name);
        if (Resource.Type.DATA_COLLECTION == resource.getResourceType()) {
          var dataCollectionWorkspace =
              attemptToGetDataCollectionWorkspace((DataCollection) resource);
          if (dataCollectionWorkspace != null) {
            getGitRepoResourceToClone(dataCollectionWorkspace.getResources());
          }
        } else {
          cloneGitRepoResource(Context.requireWorkspace().getResource(name));
        }
      }
      return;
    }
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }

  private void validateCloneCommand() {
    if (command.size() != 2 || !command.get(1).equals("clone")) {
      throw new UserActionableException(
          "Did you mean to clone git repo resources in the workspace? If so, please use terra git clone");
    }
  }

  private Workspace attemptToGetDataCollectionWorkspace(DataCollection dataCollection) {
    Workspace dataCollectionWorkspace = null;
    try {
      dataCollectionWorkspace = dataCollection.getDataCollectionWorkspace();
    } catch (SystemException e) {
      // If a user does not have access to the data collection, do not throw.
      logger.warn(String.format("Failed to get Data collection %s", dataCollection.getName()));
    }
    return dataCollectionWorkspace;
  }

  private void getGitRepoResourceToClone(List<Resource> resources) {
    resources.stream()
        .filter(resource -> Resource.Type.GIT_REPO == resource.getResourceType())
        .forEach(this::cloneGitRepoResource);
  }

  private void cloneGitRepoResource(Resource resource) {
    List<String> cloneCommands = Stream.of("git", "clone").collect(Collectors.toList());
    if (!Resource.Type.GIT_REPO.equals(resource.getResourceType())) {
      throw new UserActionableException(
          String.format(
              "%s %s cannot be cloned because it is not a git repo resource",
              resource.getResourceType(), resource.getName()));
    }
    String gitRepoUrl = resource.resolve();
    cloneCommands.add(gitRepoUrl);
    try {
      Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(cloneCommands);
    } catch (PassthroughException e) {
      ERR.println("Git clone for " + gitRepoUrl + " failed");
    }
  }
}
