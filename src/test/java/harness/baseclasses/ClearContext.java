package harness.baseclasses;

import harness.TestContext;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Base class for tests that includes standard setup/cleanup. */
public class ClearContext {
  @BeforeEach
  protected void setupEachTime() throws IOException {
    TestContext.resetGlobalContext();
  }

  @AfterEach
  protected void cleanupEachTime() throws IOException {
    TestContext.deleteGlobalContext();
  }
}
