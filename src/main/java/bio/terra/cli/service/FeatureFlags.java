package bio.terra.cli.service;

public class FeatureFlags {

  /**
   * whether auth0 response contains a refresh token which we use to get a newer access token. scope
   * needs to include "offline_access" to receive a refresh token from Auth0.
   *
   * @return false by default.
   */
  public static boolean isAuth0RefreshTokenEnabled() {
    return FeatureService.fromContext()
        .isFeatureEnabled("vwb__cli_token_refresh_enabled")
        .orElse(false);
  }

  /**
   * whether to enable dataproc commands.
   *
   * @return false by default.
   */
  public static boolean isDataprocEnabled() {
    return FeatureService.fromContext().isFeatureEnabled("vwb__cli_dataproc_enabled").orElse(false);
  }
}
