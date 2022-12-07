package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.serialization.persisted.PDServer;
import bio.terra.cli.service.DataRepoService;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.datarepo.model.RepositoryStatusModel;
import bio.terra.workspace.model.CloudPlatform;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a server. An instance of this class is part of the current context or
 * state.
 */
public class Server {
  @VisibleForTesting public static final String RESOURCE_DIRECTORY = "servers";
  @VisibleForTesting public static final String ALL_SERVERS_FILENAME = "all-servers.json";
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  private static final String DEFAULT_SERVER_FILENAME = "broad-dev-cli-testing.json";
  // unique identifier that matches the JSON file name under resources/servers.
  // (e.g. broad-dev)
  private final String name;
  private final String clientCredentialsFile;
  // Whether workspace GCP projects have cloudbuild.googleapis.com and
  // containerregistry.googleapis.com permissions.
  // TODO(PF-2199) After Broad deployments have Cloud Build enabled, this setting should be removed.
  private final boolean cloudBuildEnabled;
  // free-form text field that indicates what the server is used for
  // (e.g. Terra for development purposes)
  private final String description;
  // Terra services: information required to hit service endpoints
  // (e.g. URLs, WSM single spend profile)
  private final String samUri;
  private final boolean samInviteRequiresAdmin;
  private final String workspaceManagerUri;
  private final String wsmDefaultSpendProfile;
  private final String dataRepoUri;
  private final String externalCredsUri;
  // Terra services in the service instance are configured to accept JWT ID tokens for
  // authentication.
  private final boolean supportsIdToken;
  private final List<CloudPlatform> supportedCloudPlatforms;

  /** Build an instance of this class from the serialized format on disk. */
  public Server(PDServer configFromDisk) {
    this.name = configFromDisk.name;
    this.clientCredentialsFile = configFromDisk.clientCredentialsFile;
    this.cloudBuildEnabled = configFromDisk.cloudBuildEnabled;
    this.description = configFromDisk.description;
    this.samUri = configFromDisk.samUri;
    this.samInviteRequiresAdmin = configFromDisk.samInviteRequiresAdmin;
    this.workspaceManagerUri = configFromDisk.workspaceManagerUri;
    this.wsmDefaultSpendProfile = configFromDisk.wsmDefaultSpendProfile;
    this.dataRepoUri = configFromDisk.dataRepoUri;
    this.externalCredsUri = configFromDisk.externalCredsUri;
    this.supportsIdToken = configFromDisk.supportsIdToken;
    this.supportedCloudPlatforms = configFromDisk.supportedCloudPlatforms;
  }

  /** Return an instance of this class with default values. */
  public Server() {
    this(fromJsonFile(DEFAULT_SERVER_FILENAME));
  }

  /** Return an instance of this class from the given server name. */
  public static Server get(String name) {
    name = name.endsWith(".json") ? name : name + ".json";
    return new Server(fromJsonFile(name));
  }

  /** List all server specifications found on the classpath. */
  public static List<Server> list() {
    try {
      // read in the list of servers file
      InputStream inputStream =
          FileUtils.getResourceFileHandle(RESOURCE_DIRECTORY + "/" + ALL_SERVERS_FILENAME);
      List<String> allServerFileNames =
          JacksonMapper.getMapper().readValue(inputStream, List.class);

      // loop through the file names, reading in from JSON
      List<Server> servers = new ArrayList<>();
      for (String serverFileName : allServerFileNames) {
        servers.add(new Server(fromJsonFile(serverFileName)));
      }
      return servers;
    } catch (IOException ioEx) {
      throw new SystemException("Error reading in all possible servers.", ioEx);
    }
  }

  /**
   * Read an instance of this class in from a JSON-formatted file. This method first checks for a
   * {@link #RESOURCE_DIRECTORY}/[filename] resource on the classpath. If that file is not found,
   * then it tries to interpret [filename] as an absolute path.
   *
   * @param fileName file name
   * @return an instance of this class
   */
  @VisibleForTesting
  public static PDServer fromJsonFile(String fileName) {
    PDServer server;
    try {
      try {
        // first check for a servers/[filename] resource on the classpath
        InputStream inputStream =
            FileUtils.getResourceFileHandle(RESOURCE_DIRECTORY + "/" + fileName);
        server = JacksonMapper.getMapper().readValue(inputStream, PDServer.class);

      } catch (FileNotFoundException fnfEx) {
        // second treat the [filename] as an absolute path
        logger.debug(
            "Server file ({}) not found in resource directory, now trying as absolute path.",
            fileName);
        server = JacksonMapper.readFileIntoJavaObject(new File(fileName), PDServer.class);
      }
    } catch (IOException ioEx) {
      throw new SystemException("Error reading server file: " + fileName, ioEx);
    }

    logger.debug(
        "TEST--> server: {}, SupportedCloudPlatforms: {}",
        server.name,
        server.supportedCloudPlatforms);

    // third set up completion candidates for the command line
    // CloudPlatformCandidates.setSupportedCloudPlatforms(server.supportedCloudPlatforms);

    return server;
  }

  /**
   * Ping the service URLs to check their status. Return true if all return OK.
   *
   * <p>Each of the status checks in this method swallow all exceptions. This means that the CLI
   * treats all network or API exceptions when calling "/status" the same as a bad status code.
   */
  public boolean ping() {
    boolean samUriSpecified = (samUri != null);
    SystemStatus samStatus = null;
    if (samUriSpecified) {
      try {
        samStatus = SamService.unauthenticated(this).getStatus();
        logger.info("SAM status: {}", samStatus);
      } catch (Exception ex) {
        logger.error("Error getting SAM status.", ex);
      }
    }

    boolean wsmUriSpecified = (workspaceManagerUri != null);
    boolean wsmStatusIsOk = true;
    if (wsmUriSpecified) {
      try {
        WorkspaceManagerService.unauthenticated(this).getStatus();
        logger.info("WSM status: {}", wsmStatusIsOk);
      } catch (Exception ex) {
        logger.error("Error getting WSM status.", ex);
        wsmStatusIsOk = false;
      }
    }

    boolean dataRepoUriSpecified = (dataRepoUri != null);
    RepositoryStatusModel tdrStatus = null;
    if (dataRepoUriSpecified) {
      try {
        tdrStatus = DataRepoService.unauthenticated(this).getStatus();
        logger.info("TDR status: {}", tdrStatus);
      } catch (Exception ex) {
        logger.error("Error getting TDR status.", ex);
      }
    }

    return (!samUriSpecified || (samStatus != null && samStatus.getOk()))
        && (!wsmUriSpecified || wsmStatusIsOk)
        && (!dataRepoUriSpecified || (tdrStatus != null && tdrStatus.isOk()));
  }

  // ====================================================
  // Property getters.
  public String getName() {
    return name;
  }

  public String getClientCredentialsFile() {
    return clientCredentialsFile;
  }

  public boolean getCloudBuildEnabled() {
    return cloudBuildEnabled;
  }

  public String getDescription() {
    return description;
  }

  public String getSamUri() {
    return samUri;
  }

  public boolean getSamInviteRequiresAdmin() {
    return samInviteRequiresAdmin;
  }

  public String getWorkspaceManagerUri() {
    return workspaceManagerUri;
  }

  // TODO(PF-2257): Delete wsmDefaultSpendProfile. Profile option default is used instead.
  public String getWsmDefaultSpendProfile() {
    return wsmDefaultSpendProfile;
  }

  public String getDataRepoUri() {
    return dataRepoUri;
  }

  public String getExternalCredsUri() {
    return externalCredsUri;
  }

  public boolean getSupportsIdToken() {
    return supportsIdToken;
  }

  public List<CloudPlatform> getSupportedCloudPlatforms() {
    return supportedCloudPlatforms;
  }

  /*
  public static class CloudPlatformCandidates implements Iterable<String> {
    static java.util.List<String> supportedCloudPlatforms = new ArrayList<>();

    public static void setSupportedCloudPlatforms(List<CloudPlatform> cloudPlatforms) {
      supportedCloudPlatforms =
          cloudPlatforms.stream().map(Objects::toString).collect(Collectors.toList());
    }

    @Override
    public Iterator<String> iterator() {
      return supportedCloudPlatforms.iterator();
    }
  }

   */
}
