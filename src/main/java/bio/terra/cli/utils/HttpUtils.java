package bio.terra.cli.utils;

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
import java.util.Map;

public class HttpUtils {

  private HttpUtils() {}

  /** This is a POJO class to hold the HTTP status code and the raw JSON response body. */
  public static class HttpResponse {
    public String responseBody;
    public int statusCode;

    HttpResponse(String responseBody, int statusCode) {
      this.responseBody = responseBody;
      this.statusCode = statusCode;
    }
  }

  /**
   * Sends an HTTP request using Java's HTTPURLConnection class.
   *
   * @param urlStr where to direct the request
   * @param requestType the type of request, GET/PUT/POST/DELETE
   * @param accessToken the bearer token to include in the request, null if not required
   * @param params map of request parameters
   * @return a POJO that includes the HTTP status code and the raw JSON response body
   * @throws IOException
   */
  public static HttpResponse sendHttpRequest(
      String urlStr, String requestType, String accessToken, Map<String, String> params)
      throws IOException {
    // build parameter string
    boolean hasParams = params != null && params.size() > 0;
    String paramsStr = "";
    if (hasParams) {
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
    if (requestType.equals("GET") && hasParams) {
      urlStr += "?" + paramsStr;
    }

    // open HTTP connection
    URL url = new URL(urlStr);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    // set header properties
    //    con.setRequestProperty("Content-Type", "*/*");
    con.setRequestMethod(requestType);
    con.setRequestProperty("accept", "*/*");
    if (accessToken != null) {
      con.setRequestProperty("Authorization", "Bearer " + accessToken);
    }

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

    // return a POJO that includes both the response body and status code
    return new HttpResponse(responseBody.toString(), statusCode);
  }
}
