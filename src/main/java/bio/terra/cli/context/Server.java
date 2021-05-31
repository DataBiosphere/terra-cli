package bio.terra.cli.context;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.context.utils.JacksonMapper;
import bio.terra.cli.service.DataRepoService;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.datarepo.model.RepositoryStatusModel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * An instance of this class represents a single Terra environment or deployment. It contains all
 * the information a client would need to talk to the services. This includes the service URIs and
 * any additional information required to understand the connections between services.
 */
public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);

  // The name is a unique descriptive identifier that matches the JSON file under resources/servers.
  // (e.g. terra-dev)
  public final String name;

  // The description is a longer free-form text field that includes what the server is used for.
  // (e.g. Terra for development purposes)
  public final String description;

  // Terra services: information required to hit service endpoints
  public final String samUri;
  public final String workspaceManagerUri;
  public final String dataRepoUri;

  public static final String RESOURCE_DIRECTORY = "servers";
  public static final String ALL_SERVERS_FILENAME = "all-servers.json";

  // server that the CLI points to by default
  private static final String DEFAULT_SERVER_FILENAME = "verily-cli.json";

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  private Server(
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("samUri") String samUri,
      @JsonProperty("workspaceManagerUri") String workspaceManagerUri,
      @JsonProperty("dataRepoUri") String dataRepoUri) {
    this.name = name;
    this.description = description;
    this.samUri = samUri;
    this.workspaceManagerUri = workspaceManagerUri;
    this.dataRepoUri = dataRepoUri;
  }

  /**
   * Read an instance of this class in from a JSON-formatted file. This method first checks for a
   * {@link #RESOURCE_DIRECTORY}/[filename] resource on the classpath. If that file is not found,
   * then it tries to interpret [filename] as an absolute path.
   *
   * @param fileName file name
   * @return an instance of this class
   */
  public static Server fromJSONFile(String fileName) {
    Server server;
    try {
      try {
        // first check for a servers/[filename] resource on the classpath
        InputStream inputStream =
            FileUtils.getResourceFileHandle(RESOURCE_DIRECTORY + "/" + fileName);
        server = JacksonMapper.getMapper().readValue(inputStream, Server.class);

      } catch (FileNotFoundException fnfEx) {
        // second treat the [filename] as an absolute path
        logger.debug(
            "Server file ({}) not found in resource directory, now trying as absolute path.",
            fileName);
        server = JacksonMapper.readFileIntoJavaObject(new File(fileName), Server.class);
      }
    } catch (IOException ioEx) {
      throw new SystemException("Error reading in server: " + fileName, ioEx);
    }

    server.validate();
    return server;
  }

  /**
   * Returns the default server specification, or null if there was an error reading it in from
   * file.
   */
  public static Server getDefault() {
    return fromJSONFile(DEFAULT_SERVER_FILENAME);
  }

  /**
   * Update the server property of the global context. First treat the argument as the name of a
   * server specification found on the classpath. If that doesn't match any servers, then treat the
   * argument as an absolute path to a server specification.
   *
   * @param name name of the server to switch to
   * @return true if the server property was updated, false otherwise
   */
  public static Server switchTo(String name) {
    // lookup the server file on disk, then update the global context server property
    name = name.endsWith(".json") ? name : name + ".json";
    Server newServer = Server.fromJSONFile(name);
    GlobalContext.get().updateServer(newServer);
    return newServer;
  }

  /** Validate this server specification. */
  public void validate() {
    // check for null properties
    if (name == null || name.isEmpty()) {
      throw new SystemException("Server name cannot be empty.");
    } else if (description == null || description.isEmpty()) {
      throw new SystemException("Server description cannot be empty.");
    } else if (samUri == null || samUri.isEmpty()) {
      throw new SystemException("SAM uri cannot be empty.");
    } else if (workspaceManagerUri == null || workspaceManagerUri.isEmpty()) {
      throw new SystemException("Workspace Manager uri cannot be empty.");
    } else if (dataRepoUri == null || dataRepoUri.isEmpty()) {
      throw new SystemException("Data Repo uri cannot be empty.");
    }
  }

  /** List all server specifications found on the classpath. */
  public static List<Server> list() {
    try {
      // read in the list of servers file
      InputStream inputStream =
          FileUtils.getResourceFileHandle(
              Server.RESOURCE_DIRECTORY + "/" + Server.ALL_SERVERS_FILENAME);
      List<String> allServerFileNames =
          JacksonMapper.getMapper().readValue(inputStream, List.class);

      // loop through the file names, reading in from JSON
      List<Server> servers = new ArrayList<>();
      for (String serverFileName : allServerFileNames) {
        servers.add(Server.fromJSONFile(serverFileName));
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

    bio.terra.workspace.model.SystemStatus wsmStatus = null;
    try {
      wsmStatus = new WorkspaceManagerService(GlobalContext.get().getServer(), null).getStatus();
      logger.info("WSM status: {}", wsmStatus);
    } catch (Exception ex) {
      logger.error("Error getting WSM status.", ex);
    }

    RepositoryStatusModel tdrStatus = null;
    try {
      tdrStatus = new DataRepoService(this).getStatus();
      logger.info("TDR status: {}", tdrStatus);
    } catch (Exception ex) {
      logger.error("Error getting TDR status.", ex);
    }

    return (samStatus != null && samStatus.getOk())
        && (wsmStatus != null && wsmStatus.isOk())
        && (tdrStatus != null && tdrStatus.isOk());
  }
}
