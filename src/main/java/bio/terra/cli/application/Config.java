package bio.terra.cli.application;

public final class Config {
  // placeholder class for configuration parameters
  // these parameters will likely be read from a file or fetched from a profile later

  private static final String ClientSecretFilePath =
      "/Users/marikomedlock/.jadecli/client/jadecli_client_secret.json";
  private static final String CredentialsDirectory = "/Users/marikomedlock/.jadecli/creds/";

  private Config() {}

  public static String getClientSecretFilePath() {
    return ClientSecretFilePath;
  }

  public static String getCredentialsDirectory() {
    return CredentialsDirectory;
  }
}
