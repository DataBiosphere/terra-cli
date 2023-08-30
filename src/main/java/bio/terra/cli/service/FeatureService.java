package bio.terra.cli.service;

import static org.slf4j.LoggerFactory.*;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;

public class FeatureService {
  private static final Logger LOGGER = getLogger(FeatureService.class);
  private final FlagsmithClient flagsmith;

  // list of features
  public enum Features {
    AWS_ENABLED("vwb__aws_enabled"),
    GCP_ENABLED("vwb__gcp_enabled"),

    // CLI specific
    CLI_AUTH0_TOKEN_REFRESH_ENABLED("vwb__cli_token_refresh_enabled"),
    CLI_DATAPROC_ENABLED("vwb__cli_dataproc_enabled");

    private final String featureName;

    Features(String featureName) {
      this.featureName = featureName;
    }

    public String toString() {
      return featureName;
    }
  }

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

  public boolean isFeatureEnabled(Features feature) {
    return isFeatureEnabled(feature.featureName, /*userEmail=*/ null).orElse(false);
  }

  public boolean isFeatureEnabled(Features feature, @Nullable String userEmail) {
    return isFeatureEnabled(feature.featureName, userEmail).orElse(false);
  }

  /**
   * If Flagsmith is unavailable or the feature does not exist, return {@code Optional.empty()}
   *
   * @param feature the name of the feature
   */
  public Optional<Boolean> isFeatureEnabled(String feature, @Nullable String userEmail) {
    if (flagsmith == null) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return Optional.empty();
    }
    try {
      return Optional.of(getFlags(flagsmith, userEmail).isFeatureEnabled(feature));
    } catch (Exception e) {
      LOGGER.debug("failed to fetch feature flag value", e);
      return Optional.empty();
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
