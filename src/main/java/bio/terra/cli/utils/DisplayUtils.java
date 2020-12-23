package bio.terra.cli.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DisplayUtils {

  private DisplayUtils() {}

  public static String prettyPrintJson(Object obj) {
    try {
      String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
      return json;
    } catch (JsonProcessingException jsonEx) {
      return buildJsonError("error pretty printing json", jsonEx);
    }
  }

  public static String buildJsonError(String message, Exception ex) {
    return "{\"message\":\""
        + message
        + "\", "
        + "\"exception_message\":\""
        + ex.getMessage()
        + "\"}";
  }
}
