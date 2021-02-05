package bio.terra.cli.apps.interfaces;

import bio.terra.cli.apps.NextflowRunner;

/** Classes that implement this interface can be stopped with the 'terra app stop __' command. */
public interface Stop extends Supported {
  /** Do any command-specific cleanup or teardown. */
  void stop();

  /**
   * This enum sets the possible arguments for the 'terra app stop __' command. Classes that
   * implement this interface should also be added to this enum.
   */
  public enum StopApp {
    nextflow(new NextflowRunner());

    private Stop appHelper;

    StopApp(Stop appHelper) {
      this.appHelper = appHelper;
    }

    public Stop getAppHelper() {
      return appHelper;
    }
  }
}
