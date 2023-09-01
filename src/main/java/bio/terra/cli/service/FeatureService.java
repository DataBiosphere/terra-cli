package bio.terra.cli.service;

import static org.slf4j.LoggerFactory.*;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;

// TODO(BENCH-1050) use common code + exception from TCL

public class FeatureService {
  private static final Logger LOGGER = getLogger(FeatureService.class);

  // list of features
  public static final String AWS_ENABLED = "vwb__aws_enabled";
  public static final String CLI_AUTH0_TOKEN_REFRESH_ENABLED = "vwb__cli_token_refresh_enabled";
  public static final String CLI_DATAPROC_ENABLED = "vwb__cli_dataproc_enabled";
  private final FlagsmithClient flagsmith;

  private FeatureService(Server server) {
    if (!TextUtils.isEmpty(server.getFlagsmithApiUrl())) {
      flagsmith =
          FlagsmithClient.newBuilder()
              .withApiUrl(server.getFlagsmithApiUrl())
              .setApiKey(server.getFlagsmithClientSideKey())
              .build();
    } else {
      flagsmith = null;
    }
  }

  public static FeatureService fromContext() {
    return new FeatureService(Context.getServer());
  }

  public boolean isFeatureEnabled(String featureName) {
    return isFeatureEnabled(featureName, /*userEmail=*/ null);
  }

  public boolean isFeatureEnabled(String featureName, @Nullable String userEmail) {
    if (flagsmith == null) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return false;
    }

    try {
      return getFlags(flagsmith, userEmail).isFeatureEnabled(featureName);
    } catch (Exception e) {
      LOGGER.debug("failed to fetch feature flag value", e);
      return false;
    }
  }

  private static Flags getFlags(FlagsmithClient flagsmith, String userEmail)
      throws FlagsmithClientError {
    if (userEmail == null) {
      return flagsmith.getEnvironmentFlags();
    }
    Map<String, Object> traits = new HashMap<>();
    traits.put("email_address", userEmail);
    return flagsmith.getIdentityFlags(userEmail, traits);
  }
}
