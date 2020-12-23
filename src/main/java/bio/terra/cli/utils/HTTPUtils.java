package bio.terra.cli.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/** Utility functions to call REST endpoints. */
public final class HTTPUtils {

  private HTTPUtils() {}

  /**
   * Sends an HTTP request using Java's HTTPURLConnection class.
   *
   * @param urlStr where to direct the request
   * @param requestType the type of request, GET/PUT/POST/DELETE
   * @param params map of request parameters
   * @return a Java Map that includes the HTTP status code (under statusCode key) and the parsed
   *     JSON response
   * @throws IOException
   */
  public static Map<String, Object> sendJavaHttpRequest(
      String urlStr, String requestType, Map<String, String> params) throws IOException {
    // build parameter string
    String paramsStr = "";
    if (params != null && params.size() > 0) {
      StringBuilder paramsStrBuilder = new StringBuilder();
      for (Map.Entry<String, String> mapEntry : params.entrySet()) {
        paramsStrBuilder.append(URLEncoder.encode(mapEntry.getKey(), "UTF-8"));
        paramsStrBuilder.append("=");
        paramsStrBuilder.append(URLEncoder.encode(mapEntry.getValue(), "UTF-8"));
        paramsStrBuilder.append("&");
      }
      paramsStr = paramsStrBuilder.toString();
    }

    // for GET requests, append the parameters to the URL
    if (requestType.equals("GET")) {
      urlStr += "?" + paramsStr;
    }

    // open HTTP connection
    URL url = new URL(urlStr);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    // set header properties
    con.setRequestProperty("Content-Type", "application/json");
    con.setRequestMethod(requestType);

    // for other request types, write the parameters to the request body
    if (!requestType.equals("GET")) {
      con.setDoOutput(true);
      DataOutputStream outputStream = new DataOutputStream(con.getOutputStream());
      outputStream.writeBytes(paramsStr);
      outputStream.flush();
      outputStream.close();
    }

    // send the request and read the returned status code
    int statusCode = con.getResponseCode();

    // select the appropriate input stream depending on the status code
    InputStream inputStream;
    if (HttpStatusCodes.isSuccess(statusCode)) {
      inputStream = con.getInputStream();
    } else {
      inputStream = con.getErrorStream();
    }

    // read the response body
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
    String inputLine;
    StringBuffer responseBody = new StringBuffer();
    while ((inputLine = bufferedReader.readLine()) != null) {
      responseBody.append(inputLine);
    }
    bufferedReader.close();

    // close HTTP connection
    con.disconnect();

    // build and return the response map
    return buildMapFromJSON(responseBody.toString(), statusCode);
  }

  /**
   * Parses the HTTP response body from a Java String that uses JSON format into a Java key/value
   * Map. This method uses the Jackson JSON mapping library (ObjectMapper).
   *
   * @param responseBody the response body as a String that uses JSON format
   * @param statusCode the HTTP status code as an Integer, ignored if null
   * @return a Java Map that includes the HTTP status code (under statusCode key) and the parsed
   *     JSON response
   * @throws JsonProcessingException
   */
  private static Map<String, Object> buildMapFromJSON(String responseBody, Integer statusCode)
      throws JsonProcessingException {
    // JSON parse the response body into a Java Map
    Map<String, Object> map;
    if (responseBody.equals("")) {
      // create an empty map if no response body
      map = new HashMap<String, Object>();
    } else {
      // create and populate a map using the Jackson JSON mapping library
      ObjectMapper objMapper = new ObjectMapper();
      map = objMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
    }

    if (statusCode != null) {
      // put the status code into the response map
      map.put("statusCode", statusCode);
    }

    return map;
  }
}
