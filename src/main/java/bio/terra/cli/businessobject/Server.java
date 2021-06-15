package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.serialization.persisted.PDServer;
import bio.terra.cli.service.DataRepoService;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.datarepo.model.RepositoryStatusModel;
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
  private static final Logger logger = LoggerFactory.getLogger(Server.class);

  // unique identifier that matches the JSON file name under resources/servers.
  // (e.g. terra-dev)
  private String name;

  // free-form text field that indicates what the server is used for
  // (e.g. Terra for development purposes)
  private String description;

  // Terra services: information required to hit service endpoints
  // (e.g. URLs, WSM single spend profile)
  private String samUri;
  private String workspaceManagerUri;
  private String dataRepoUri;

  private static final String DEFAULT_SERVER_FILENAME = "verily-cli.json";
  private static final String RESOURCE_DIRECTORY = "servers";
  private static final String ALL_SERVERS_FILENAME = "all-servers.json";

  /** Build an instance of this class from the serialized format on disk. */
  public Server(PDServer configFromDisk) {
    this.name = configFromDisk.name;
    this.description = configFromDisk.description;
    this.samUri = configFromDisk.samUri;
    this.workspaceManagerUri = configFromDisk.workspaceManagerUri;
    this.dataRepoUri = configFromDisk.dataRepoUri;
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
   * Ping the service URLs to check their status. Return true if all return OK.
   *
   * <p>Each of the status checks in this method swallow all exceptions. This means that the CLI
   * treats all network or API exceptions when calling "/status" the same as a bad status code.
   */
  public boolean ping() {
    SystemStatus samStatus = null;
    try {
      samStatus = new SamService(this).getStatus();
      logger.info("SAM status: {}", samStatus);
    } catch (Exception ex) {
      logger.error("Error getting SAM status.", ex);
    }

    boolean wsmStatusIsOk = true;
    try {
      new WorkspaceManagerService(null).getStatus();
      logger.info("WSM status: {}", wsmStatusIsOk);
    } catch (Exception ex) {
      logger.error("Error getting WSM status.", ex);
      wsmStatusIsOk = false;
    }

    RepositoryStatusModel tdrStatus = null;
    try {
      tdrStatus = new DataRepoService(this).getStatus();
      logger.info("TDR status: {}", tdrStatus);
    } catch (Exception ex) {
      logger.error("Error getting TDR status.", ex);
    }

    return (samStatus != null && samStatus.getOk())
        && (wsmStatusIsOk)
        && (tdrStatus != null && tdrStatus.isOk());
  }

  /**
   * Read an instance of this class in from a JSON-formatted file. This method first checks for a
   * {@link #RESOURCE_DIRECTORY}/[filename] resource on the classpath. If that file is not found,
   * then it tries to interpret [filename] as an absolute path.
   *
   * @param fileName file name
   * @return an instance of this class
   */
  private static PDServer fromJsonFile(String fileName) {
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
      throw new SystemException("Error reading in server file: " + fileName, ioEx);
    }

    return server;
  }

  // ====================================================
  // Property getters.
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getSamUri() {
    return samUri;
  }

  public String getWorkspaceManagerUri() {
    return workspaceManagerUri;
  }

  public String getDataRepoUri() {
    return dataRepoUri;
  }
}
