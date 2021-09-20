package bio.terra.cli.serialization.userfacing;

/** User-facing classes that support terminal output should implement this. */
public interface UserFacingPrintable {
  /** print in text format to the OUT PrintStream */
  void print();
}
