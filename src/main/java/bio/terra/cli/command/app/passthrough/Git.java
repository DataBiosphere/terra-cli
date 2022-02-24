package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.UserActionableException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "git", description = "Call git in the Terra workspace.")
public class Git extends ToolCommand {

  @CommandLine.Option(
      names = "--resource",
      description =
          "name of the git-repo resources in the current workspace to be cloned. \n"
              + "Example usage: git clone --resource=repo1 --resource=repo2")
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
      Context.requireWorkspace().getResources().stream()
          .filter(resource -> Resource.Type.GIT_REPO == resource.getResourceType())
          .forEach(resource -> cloneGitRepoResource(resource));
      return;
    }
    if (names != null) {
      validateCloneCommand();
      for (String name : names) {
        cloneGitRepoResource(Context.requireWorkspace().getResource(name));
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

  private void cloneGitRepoResource(Resource resource) {
    List<String> cloneCommands = Stream.of("git", "clone").collect(Collectors.toList());
    String gitRepoUrl = resource.resolve();
    cloneCommands.add(gitRepoUrl);
    try {
      Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(cloneCommands);
    } catch (PassthroughException e) {
      OUT.println("Git clone for " + gitRepoUrl + " failed");
    }
  }
}
