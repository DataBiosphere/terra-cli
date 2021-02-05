package bio.terra.cli.service;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.utils.DataRepoUtils;
import bio.terra.cli.service.utils.SamUtils;
import bio.terra.cli.service.utils.WorkspaceManagerUtils;
import bio.terra.datarepo.model.RepositoryStatusModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class manipulates the server-related properties of the global context object. */
public class ServerManager {
  private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);

  private final GlobalContext globalContext;

  public ServerManager(GlobalContext globalContext) {
    this.globalContext = globalContext;
  }

  /**
   * Update the server property of the global context. First treat the argument as the name of a
   * server specification found on the classpath. If that doesn't match an servers, then treat the
   * argument as an absolute path to a server specification.
   *
   * @return true if the server property was updated, false otherwise
   */
  public boolean updateServer(String serverName) {
    List<ServerSpecification> allPossibleServers = allPossibleServers();
    for (ServerSpecification newServer : allPossibleServers) {
      if (newServer.name.equals(serverName)) {
        // found a match, update the global context and return here
        globalContext.updateServer(newServer);
        return true;
      }
    }

    // no matches found, so try treating the argument as an absolute path
    try {
      ServerSpecification newServer = ServerSpecification.fromJSONFile(serverName);
      if (newServer != null) {
        globalContext.updateServer(newServer);
        return true;
      }
    } catch (IOException ioEx) {
    }
    logger.error("Error reading in server specification file ({}).", serverName);
    return false;
  }

  /** Ping the service URLs to check their status. Return true if all return OK. */
  public boolean pingServerStatus() {
    ApiClient samClient = SamUtils.getClientForTerraUser(null, globalContext.server);
    SystemStatus samStatus = SamUtils.getStatus(samClient);
    logger.info("SAM status: {}", samStatus);

    bio.terra.workspace.client.ApiClient wsmClient =
        WorkspaceManagerUtils.getClientForTerraUser(null, globalContext.server);
    bio.terra.workspace.model.SystemStatus wsmStatus = WorkspaceManagerUtils.getStatus(wsmClient);
    logger.info("Workspace Manager status: {}", wsmStatus);

    bio.terra.datarepo.client.ApiClient tdrClient =
        DataRepoUtils.getClientForTerraUser(null, globalContext.server);
    RepositoryStatusModel tdrStatus = DataRepoUtils.getStatus(tdrClient);
    logger.info("Data Repo status: {}", tdrStatus);

    return (samStatus != null && samStatus.getOk())
        && (wsmStatus != null && wsmStatus.isOk())
        && (tdrStatus != null && tdrStatus.isOk());
  }

  // This variable defines the server that the CLI points to by default.
  private static final String DEFAULT_SERVER_FILENAME = "terra-dev.json";

  /**
   * Returns the default server specification, or null if there was an error reading it in from
   * file.
   */
  public static ServerSpecification defaultServer() {
    try {
      return ServerSpecification.fromJSONFile(DEFAULT_SERVER_FILENAME);
    } catch (IOException ioEx) {
      logger.error("Error reading in default server file. ({})", DEFAULT_SERVER_FILENAME, ioEx);
      return null;
    }
  }

  /** List all server specifications found on the classpath. */
  public static List<ServerSpecification> allPossibleServers() {
    // use Jackson to map the stream contents to a list of strings
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      // read in the list of servers file
      InputStream inputStream =
          FileUtils.getResourceFileHandle(
              ServerSpecification.RESOURCE_DIRECTORY
                  + "/"
                  + ServerSpecification.ALL_SERVERS_FILENAME);
      List<String> allServerFileNames = objectMapper.readValue(inputStream, List.class);

      // loop through the file names, reading in from JSON
      List<ServerSpecification> servers = new ArrayList<>();
      for (String serverFileName : allServerFileNames) {
        servers.add(ServerSpecification.fromJSONFile(serverFileName));
      }
      return servers;
    } catch (IOException ioEx) {
      logger.error("Error reading in all possible servers.", ioEx);
      return new ArrayList<>();
    }
  }
}
