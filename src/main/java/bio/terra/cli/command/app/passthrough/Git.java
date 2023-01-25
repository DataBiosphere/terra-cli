package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.UserActionableException;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

  private Set<String> getGitReposInWorkspace() {
    return Context.requireWorkspace().listResources().stream()
        .filter(resource -> Resource.Type.GIT_REPO == resource.getResourceType())
        .map(Resource::resolve)
        .collect(Collectors.toSet());
  }

  private Set<String> getGitReposByNames() {
    Set<String> gitResources = new HashSet<>();
    Arrays.stream(names)
        .forEach(
            name -> {
              var resource = Context.requireWorkspace().getResource(name);
              if (Resource.Type.GIT_REPO != resource.getResourceType()) {
                throw new UserActionableException(
                    String.format(
                        "%s %s cannot be cloned because it is not a git resource",
                        resource.getResourceType(), resource.getName()));
              }
              gitResources.add(resource.resolve());
            });
    return gitResources;
  }

  private void clone(Set<String> gitRepos) {
    gitRepos.forEach(
        gitRepo -> {
          List<String> cloneCommands = ImmutableList.of("git", "clone", gitRepo);
          try {
            Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(cloneCommands);
          } catch (PassthroughException e) {
            ERR.println("Git clone for " + gitRepo + " failed");
          }
        });
  }
}
