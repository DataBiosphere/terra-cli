package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.UserActionableException;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "git", description = "Call git in the Terra workspace.")
public class Git extends ToolCommand {
  private static final Logger logger = LoggerFactory.getLogger(Git.class);

  private record Repo(String resourceName, String url) {}

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
      clone(cloneAll ? getGitReposInWorkspace() : getGitReposByNames());
      return;
    }

    // handle other git commands
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }

  private void validateCloneCommand() {
    if (command.size() != 2 || !command.get(1).equals("clone")) {
      throw new UserActionableException(
          "Did you mean to clone git repo resources in the workspace? If so, please use terra git clone");
    }
  }

  private List<Repo> getGitReposInWorkspace() {
    return Context.requireWorkspace().listResources().stream()
        .filter(resource -> Resource.Type.GIT_REPO == resource.getResourceType())
        .map(resource -> new Repo(resource.getName(), resource.resolve()))
        .toList();
  }

  private List<Repo> getGitReposByNames() {
    return Arrays.stream(names)
        .map(
            name -> {
              Resource resource = Context.requireWorkspace().getResource(name);
              if (Resource.Type.GIT_REPO != resource.getResourceType()) {
                throw new UserActionableException(
                    String.format(
                        "%s %s cannot be cloned because it is not a git resource",
                        resource.getResourceType(), resource.getName()));
              }
              return new Repo(resource.getName(), resource.resolve());
            }).toList();
  }

  private void clone(List<Repo> gitRepos) {
    gitRepos.forEach(
        gitRepo -> {
          List<String> cloneCommands = ImmutableList.of("git", "clone", gitRepo.url, gitRepo.resourceName);
          try {
            Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(cloneCommands);
          } catch (PassthroughException e) {
            ERR.println("Git clone for " + gitRepo + " failed");
          }
        });
  }
}
