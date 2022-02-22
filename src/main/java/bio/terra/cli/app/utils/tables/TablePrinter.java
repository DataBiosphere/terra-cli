package bio.terra.cli.app.utils.tables;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import java.util.Arrays;
import java.util.List;

public class TablePrinter {

  private static final TablePrintable<UFWorkspace> workspacePrinter = UFWorkspaceColumns::values;
  public static <E extends Enum<E> & TablePrintable<?>, T> String print(E printable, List<T> rowObjects) {
//    String headerRow = Arrays.stream(printable::values)
//        .map()
  }
}
