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
public class Syntax extends ClearContextUnit {

  @Test
  @DisplayName("format options are accepted in any case")
  void formatOptions() {
    Result resultLowercase = TestCommand.runCommand("status", "--format=json");
    assertThat(resultLowercase.stdOut, containsString("\"workspace\" :"));

    Result resultUppercase = TestCommand.runCommand("status", "--format=JSON");
    assertThat(resultUppercase.stdOut, containsString("\"workspace\" :"));

    Result resultMixed = TestCommand.runCommand("status", "--format=jSON");
    assertThat(resultMixed.stdOut, containsString("\"workspace\" :"));
  }

  @Test
  @DisplayName("config value enums are accepted in any case")
  void configOptions() {
    Result resultLowercase = TestCommand.runCommand("config", "set", "browser", "manual");
    assertThat(resultLowercase.stdOut, containsString("Browser launch mode for login is MANUAL"));

    Result resultUppercase = TestCommand.runCommand("config", "set", "browser", "MANUAL");
    assertThat(resultUppercase.stdOut, containsString("Browser launch mode for login is MANUAL"));

    Result resultMixed = TestCommand.runCommand("config", "set", "browser", "mAnual");
    assertThat(resultMixed.stdOut, containsString("Browser launch mode for login is MANUAL"));
  }
}
