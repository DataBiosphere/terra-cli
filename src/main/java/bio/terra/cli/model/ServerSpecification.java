package bio.terra.cli.model;

import bio.terra.cli.utils.DataRepoUtils;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.SAMUtils;
import bio.terra.cli.utils.WorkspaceManagerUtils;
import bio.terra.datarepo.model.RepositoryStatusModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An instance of this class represents a single Terra environment or deployment. It contains all
 * the information a client would need to talk to the services. This includes the service URIs and
 * any additional information required to understand the connections between services.
 *
 * <p>Note: This class currently includes several properties specific to particular services (i.e.
 * everything under the "Terra services: ..." comment). I don't like this hard-coding because it
 * makes the TestRunner library "depend" on the services that it will be used to test. I think the
 * right way to do this is for all of these properties to be added to the Kubernetes config map and
 * removed from the ServerSpecification. We should only need a pointer to the appropriate Kubernetes
 * cluster to get these values.
 */
public class ServerSpecification {
  private static final Logger logger = LoggerFactory.getLogger(ServerSpecification.class);

  public String name;
  public String description = "";

  // Terra services: information required to hit service endpoints
  public String samUri;
  public String workspaceManagerUri;
  public String datarepoUri;

  public static final String RESOURCE_DIRECTORY = "servers";

  ServerSpecification() {}

  /**
   * Read an instance of this class in from a JSON-formatted file. This method expects that the file
   * name exists in the directory specified by {@link #RESOURCE_DIRECTORY}
   *
   * @param resourceFileName file name
   * @return an instance of this class
   */
  public static ServerSpecification fromJSONFile(String resourceFileName) throws IOException {
    // use Jackson to map the stream contents to a TestConfiguration object
    ObjectMapper objectMapper = new ObjectMapper();

    // read in the server file
    InputStream inputStream =
        FileUtils.getResourceFileHandle(RESOURCE_DIRECTORY + "/" + resourceFileName);
    ServerSpecification server = objectMapper.readValue(inputStream, ServerSpecification.class);

    server.validate();

    return server;
  }

  /** Validate this server specification. */
  public void validate() {
    // check for null properties
    if (name == null || name.isEmpty()) {
      throw new RuntimeException("Server name cannot be empty.");
    } else if (description == null || description.isEmpty()) {
      throw new RuntimeException("Server description cannot be empty.");
    } else if (samUri == null || samUri.isEmpty()) {
      throw new RuntimeException("SAM uri cannot be empty.");
    } else if (workspaceManagerUri == null || workspaceManagerUri.isEmpty()) {
      throw new RuntimeException("Workspace Manager uri cannot be empty.");
    } else if (datarepoUri == null || datarepoUri.isEmpty()) {
      throw new RuntimeException("Data Repo uri cannot be empty.");
    }
  }

  /** Ping the service URLs to check their status. Return true if all return OK. */
  public boolean pingServerStatus() {
    ApiClient samClient = SAMUtils.getClientForTerraUser(null, this);
    SystemStatus samStatus = SAMUtils.getStatus(samClient);
    logger.debug("SAM status: {}", samStatus);

    bio.terra.workspace.client.ApiClient wsmClient =
        WorkspaceManagerUtils.getClientForTerraUser(null, this);
    bio.terra.workspace.model.SystemStatus wsmStatus = WorkspaceManagerUtils.getStatus(wsmClient);
    logger.debug("Workspace Manager status: {}", wsmStatus);

    bio.terra.datarepo.client.ApiClient tdrClient = DataRepoUtils.getClientForTerraUser(null, this);
    RepositoryStatusModel tdrStatus = DataRepoUtils.getStatus(tdrClient);
    logger.debug("Data Repo status: {}", tdrStatus);

    return (samStatus != null && samStatus.getOk())
        && (wsmStatus != null && wsmStatus.isOk())
        && (tdrStatus != null && tdrStatus.isOk());
  }
}
