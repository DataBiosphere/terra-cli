package harness.utils;

import java.util.Random;

public class TestUtils {
  private static final Random RANDOM = new Random();

  public static String appendRandomNumber(String string) {
    // This is used for BQ datasets, so can't have "-". (BQ dataset names can't have "-".)
    return string + RANDOM.nextInt(10000);
  }
}
