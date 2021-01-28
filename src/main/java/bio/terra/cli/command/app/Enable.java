package bio.terra.cli.command.app;

import bio.terra.cli.app.supported.GcloudHelper;
import bio.terra.cli.app.supported.NextflowHelper;
import bio.terra.cli.app.supported.SupportedToolHelper;
import bio.terra.cli.model.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app enable" command. */
@Command(name = "enable", description = "Enable an application in the Terra workspace.")
public class Enable implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "name of the application to enable: ${COMPLETION-CANDIDATES}")
  private SupportedForEnableAndStop appName;

  @Override
  public Integer call() {
    // TODO: should we also pull the image on any call to enable?
    GlobalContext globalContext = GlobalContext.readFromFile();
    appName.getToolHelper().enable(globalContext);
    System.out.println(appName.toString() + " successfully enabled.");
    return 0;
  }

  /** This enum lists only the supported apps that can be enabled and stopped. */
  public enum SupportedForEnableAndStop {
    nextflow(new NextflowHelper()),
    gcloud(new GcloudHelper());

    private SupportedToolHelper toolHelper;

    SupportedForEnableAndStop(SupportedToolHelper toolHelper) {
      this.toolHelper = toolHelper;
    }

    public SupportedToolHelper getToolHelper() {
      return toolHelper;
    }
  }
}
