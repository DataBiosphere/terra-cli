package bio.terra.cli.service;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.utils.DataRepoService;
import bio.terra.cli.service.utils.SamService;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.datarepo.model.RepositoryStatusModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
    throw new SystemException("Error reading in server specification file (" + serverName + ").");
  }

  /**
   * Ping the service URLs to check their status. Return true if all return OK.
   *
   * <p>Each of the status checks in this method swallow all exceptions. This means that the CLI
   * treats all network or API exceptions when calling "/status" the same as a bad status code.
   */
  public boolean pingServerStatus() {
    SystemStatus samStatus = null;
    try {
      samStatus = new SamService(globalContext.server).getStatus();
      logger.info("SAM status: {}", samStatus);
    } catch (Exception ex) {
      logger.error("Error getting SAM status.", ex);
    }

    bio.terra.workspace.model.SystemStatus wsmStatus = null;
    try {
      wsmStatus = new WorkspaceManagerService(globalContext.server).getStatus();
      logger.info("WSM status: {}", wsmStatus);
    } catch (Exception ex) {
      logger.error("Error getting WSM status.", ex);
    }

    RepositoryStatusModel tdrStatus = null;
    try {
      tdrStatus = new DataRepoService(globalContext.server).getStatus();
      logger.info("TDR status: {}", tdrStatus);
    } catch (Exception ex) {
      logger.error("Error getting TDR status.", ex);
    }

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
      throw new SystemException(
          "Error reading in default server file. (" + DEFAULT_SERVER_FILENAME + ")", ioEx);
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
      throw new SystemException("Error reading in all possible servers.", ioEx);
    }
  }
}
