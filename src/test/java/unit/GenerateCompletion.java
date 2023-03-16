package unit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import harness.TestCommand;
import harness.TestCommand.Result;
import harness.baseclasses.ClearContextUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for syntax variations */
@Tag("unit")
public class GenerateCompletion extends ClearContextUnit {
  @Test
  @DisplayName("command generates output")
  void generateCompletion() {
    Result result = TestCommand.runCommandExpectSuccess("generate-completion");
    assertThat(result.stdOut, containsString("terra Bash Completion"));
    assertThat(result.stdOut, containsString("function _complete_terra"));
  }
}
