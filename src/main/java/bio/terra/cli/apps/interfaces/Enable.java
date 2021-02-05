package bio.terra.cli.apps.interfaces;

import bio.terra.cli.apps.NextflowRunner;

/** Classes that implement this interface can be enabled with the 'terra app enable __' command. */
public interface Enable extends Supported {
  /** Do any command-specific setup. */
  void enable();

  /**
   * This enum sets the possible arguments for the 'terra app enble __' command. Classes that
   * implement this interface should also be added to this enum.
   */
  public enum EnableApp {
    nextflow(new NextflowRunner());

    private Enable appHelper;

    EnableApp(Enable appHelper) {
      this.appHelper = appHelper;
    }

    public Enable getAppHelper() {
      return appHelper;
    }
  }
}
