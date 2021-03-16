package bio.terra.cli.context;

import bio.terra.cli.command.exception.InternalErrorException;
import bio.terra.cli.context.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An instance of this class represents a single Terra environment or deployment. It contains all
 * the information a client would need to talk to the services. This includes the service URIs and
 * any additional information required to understand the connections between services.
 */
public class ServerSpecification {
  private static final Logger logger = LoggerFactory.getLogger(ServerSpecification.class);

  // The name is a unique descriptive identifier that matches the JSON file under resources/servers.
  // (e.g. terra-dev)
  public String name;

  // The description is a longer free-form text field that includes what the server is used for.
  // (e.g. Terra for development purposes)
  public String description = "";

  // Terra services: information required to hit service endpoints
  public String samUri;
  public String workspaceManagerUri;
  public String dataRepoUri;

  public static final String RESOURCE_DIRECTORY = "servers";
  public static final String ALL_SERVERS_FILENAME = "all-servers.json";

  ServerSpecification() {}

  /**
   * Read an instance of this class in from a JSON-formatted file. This method first checks for a
   * {@link #RESOURCE_DIRECTORY}/[filename] resource on the classpath. If that file is not found,
   * then it tries to interpret [filename] as an absolute path.
   *
   * @param fileName file name
   * @return an instance of this class
   */
  public static ServerSpecification fromJSONFile(String fileName) throws IOException {
    // use Jackson to map the stream contents to a ServerSpecification object
    ObjectMapper objectMapper = new ObjectMapper();

    // read in the server file
    ServerSpecification server;
    try {
      // first check for a servers/[filename] resource on the classpath
      InputStream inputStream =
          FileUtils.getResourceFileHandle(RESOURCE_DIRECTORY + "/" + fileName);
      server = objectMapper.readValue(inputStream, ServerSpecification.class);
    } catch (FileNotFoundException fnfEx) {
      // second treat the [filename] as an absolute path
      logger.debug(
          "Server file ({}) not found in resource directory, now trying as absolute path.",
          fileName);
      server = FileUtils.readFileIntoJavaObject(new File(fileName), ServerSpecification.class);
    }

    if (server != null) {
      server.validate();
    }

    return server;
  }

  /** Validate this server specification. */
  public void validate() {
    // check for null properties
    if (name == null || name.isEmpty()) {
      throw new InternalErrorException("Server name cannot be empty.");
    } else if (description == null || description.isEmpty()) {
      throw new InternalErrorException("Server description cannot be empty.");
    } else if (samUri == null || samUri.isEmpty()) {
      throw new InternalErrorException("SAM uri cannot be empty.");
    } else if (workspaceManagerUri == null || workspaceManagerUri.isEmpty()) {
      throw new InternalErrorException("Workspace Manager uri cannot be empty.");
    } else if (dataRepoUri == null || dataRepoUri.isEmpty()) {
      throw new InternalErrorException("Data Repo uri cannot be empty.");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { // same object
      return true;
    }

    if (!(obj instanceof ServerSpecification)) { // wrong type
      return false;
    }

    ServerSpecification server = (ServerSpecification) obj;
    return this.name.equals(server.name)
        && this.description.equals(server.description)
        && this.samUri.equals(server.samUri)
        && this.dataRepoUri.equals(server.dataRepoUri)
        && this.workspaceManagerUri.equals(server.workspaceManagerUri);
  }

  @Override
  public int hashCode() {
    int result = 23;

    result = 11 * result + name.hashCode();
    result = 11 * result + description.hashCode();
    result = 11 * result + samUri.hashCode();
    result = 11 * result + dataRepoUri.hashCode();
    result = 11 * result + workspaceManagerUri.hashCode();

    return result;
  }
}
