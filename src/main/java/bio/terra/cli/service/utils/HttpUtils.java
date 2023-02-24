package bio.terra.cli.service.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.utils.UserIO;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for making raw HTTP requests (e.g. in place of using a client library). */
public class HttpUtils {
  // default value for the maximum number of times to retry HTTP requests
  public static final int DEFAULT_MAXIMUM_RETRIES = 15;
  // default value for the time to sleep between retries
  public static final Duration DEFAULT_DURATION_SLEEP_FOR_RETRY = Duration.ofSeconds(1);
  private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

  private HttpUtils() {}

  /**
   * Sends an HTTP request using Java's HTTPURLConnection class.
   *
   * @param urlStr where to direct the request
   * @param requestType the type of request, GET/PUT/POST/DELETE
   * @param accessToken the bearer token to include in the request, null if not required
   * @param params map of request parameters
   * @return a POJO that includes the HTTP status code and the raw JSON response body
   * @throws IOException IOException
   */
  public static HttpResponse sendHttpRequest(
      String urlStr, String requestType, String accessToken, Map<String, String> params)
      throws IOException {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "*/*");
    if (accessToken != null) {
      headers.put("Authorization", "Bearer " + accessToken);
    }

    return sendHttpRequest(urlStr, requestType, headers, params);
  }

  /**
   * Sends an HTTP request using Java's HTTPURLConnection class.
   *
   * @param urlStr where to direct the request
   * @param requestType the type of request, GET/PUT/POST/DELETE
   * @param headers map of request headers
   * @param params map of request parameters
   * @return a POJO that includes the HTTP status code and the raw JSON response body
   * @throws IOException IOException
   */
  public static HttpResponse sendHttpRequest(
      String urlStr, String requestType, Map<String, String> headers, Map<String, String> params)
      throws IOException {
    // build parameter string
    boolean hasParams = params != null && params.size() > 0;
    String paramsStr = "";
    if (hasParams) {
      StringBuilder paramsStrBuilder = new StringBuilder();
      for (Map.Entry<String, String> mapEntry : params.entrySet()) {
        paramsStrBuilder.append(URLEncoder.encode(mapEntry.getKey(), StandardCharsets.UTF_8));
        paramsStrBuilder.append("=");
        paramsStrBuilder.append(URLEncoder.encode(mapEntry.getValue(), StandardCharsets.UTF_8));
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
    con.setRequestMethod(requestType);
    for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
      con.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
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

  /**
   * Helper method to call a function with retries. Uses {@link #DEFAULT_MAXIMUM_RETRIES} for
   * maximum number of retries and {@link #DEFAULT_DURATION_SLEEP_FOR_RETRY} for the time to sleep
   * between retries.
   *
   * @param makeRequest function to perform the request
   * @param isRetryable function to test whether the exception is retryable or not
   * @throws E if makeRequest throws an exception that is not retryable
   * @throws SystemException if the maximum number of retries is exhausted, and the last attempt
   *     threw a retryable exception
   */
  public static <E extends Exception> void callWithRetries(
      RunnableWithCheckedException<E> makeRequest, Predicate<Exception> isRetryable)
      throws E, InterruptedException {
    callWithRetries(
        () -> {
          makeRequest.run();
          return null;
        },
        isRetryable,
        DEFAULT_MAXIMUM_RETRIES,
        DEFAULT_DURATION_SLEEP_FOR_RETRY);
  }

  /**
   * Helper method to call a function with retries. Uses {@link #DEFAULT_MAXIMUM_RETRIES} for
   * maximum number of retries and {@link #DEFAULT_DURATION_SLEEP_FOR_RETRY} for the time to sleep
   * between retries.
   *
   * @param makeRequest function to perform the request
   * @param isRetryable function to test whether the exception is retryable or not
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @return the response object
   * @throws E if makeRequest throws an exception that is not retryable
   * @throws SystemException if the maximum number of retries is exhausted, and the last attempt
   *     threw a retryable exception
   */
  public static <T, E extends Exception> T callWithRetries(
      SupplierWithCheckedException<T, E> makeRequest, Predicate<Exception> isRetryable)
      throws E, InterruptedException {
    return callWithRetries(
        makeRequest, isRetryable, DEFAULT_MAXIMUM_RETRIES, DEFAULT_DURATION_SLEEP_FOR_RETRY);
  }

  /**
   * Helper method to call a function with retries.
   *
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @param makeRequest function to perform the request
   * @param isRetryable function to test whether the exception is retryable or not
   * @param maxCalls maximum number of times to retry
   * @param sleepDuration time to sleep between tries
   * @return the response object
   * @throws E if makeRequest throws an exception that is not retryable
   * @throws SystemException if the maximum number of retries is exhausted, and the last attempt
   *     threw a retryable exception
   */
  public static <T, E extends Exception> T callWithRetries(
      SupplierWithCheckedException<T, E> makeRequest,
      Predicate<Exception> isRetryable,
      int maxCalls,
      Duration sleepDuration)
      throws E, InterruptedException {
    // isDone always return true
    return pollWithRetries(
        makeRequest,
        (result) -> true,
        isRetryable,
        /* shouldPrintToStderrOnRetry */ true,
        maxCalls,
        sleepDuration);
  }

  /**
   * Helper method to poll with retries. Uses {@link #DEFAULT_MAXIMUM_RETRIES} for maximum number of
   * retries and {@link #DEFAULT_DURATION_SLEEP_FOR_RETRY} for the time to sleep between retries.
   *
   * @param makeRequest function to perform the request
   * @param isRetryable function to test whether the exception is retryable or not
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @return the response object
   * @throws E if makeRequest throws an exception that is not retryable
   * @throws SystemException if the maximum number of retries is exhausted, and the last attempt
   *     threw a retryable exception
   */
  public static <T, E extends Exception> T pollWithRetries(
      SupplierWithCheckedException<T, E> makeRequest,
      Predicate<T> isDone,
      Predicate<Exception> isRetryable)
      throws E, InterruptedException {
    return pollWithRetries(
        makeRequest,
        isDone,
        isRetryable,
        DEFAULT_MAXIMUM_RETRIES,
        DEFAULT_DURATION_SLEEP_FOR_RETRY);
  }

  /**
   * Helper method to poll with retries.
   *
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @param makeRequest function to perform the request
   * @param isDone function to decide whether to keep polling or not, based on the result
   * @param isRetryable function to test whether the exception is retryable or not
   * @param maxCalls maximum number of times to poll or retry
   * @param sleepDuration time to sleep between tries
   * @return the response object
   * @throws E if makeRequest throws an exception that is not retryable
   * @throws SystemException if the maximum number of retries is exhausted, and the last attempt
   *     threw a retryable exception
   */
  public static <T, E extends Exception> T pollWithRetries(
      SupplierWithCheckedException<T, E> makeRequest,
      Predicate<T> isDone,
      Predicate<Exception> isRetryable,
      int maxCalls,
      Duration sleepDuration)
      throws E, InterruptedException {
    return pollWithRetries(
        makeRequest,
        isDone,
        isRetryable,
        /* shouldPrintToStderrOnRetry */ false,
        maxCalls,
        sleepDuration);
  }

  /**
   * Helper method to poll with retries.
   *
   * <p>If there is no timeout, the method returns the last result.
   *
   * <p>If there is a timeout, the behavior depends on the last attempt.
   *
   * <p>- If the last attempt produced a result that is not done (i.e. isDone returns false), then
   * the result is returned.
   *
   * <p>- If the last attempt threw a retryable exception, then this method re-throws that last
   * exception wrapped in a {@link SystemException} with a timeout message.
   *
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @param makeRequest function to perform the request
   * @param isDone function to decide whether to keep polling or not, based on the result
   * @param isRetryable function to test whether the exception is retryable or not
   * @param shouldPrintToStderrOnRetry should print to stderr on retry
   * @param maxCalls maximum number of times to poll or retry
   * @param sleepDuration time to sleep between tries
   * @return the response object
   * @throws E if makeRequest throws an exception that is not retryable
   * @throws SystemException if the maximum number of retries is exhausted, and the last attempt
   *     threw a retryable exception
   */
  public static <T, E extends Exception> T pollWithRetries(
      SupplierWithCheckedException<T, E> makeRequest,
      Predicate<T> isDone,
      Predicate<Exception> isRetryable,
      boolean shouldPrintToStderrOnRetry,
      int maxCalls,
      Duration sleepDuration)
      throws E, InterruptedException {
    int numTries = 0;
    Exception lastRetryableException = null;
    do {
      numTries++;
      try {
        logger.debug("Request attempt #{}", numTries);

        // Print to STDERR so that terminal command doesn't appear to hang.
        if (shouldPrintToStderrOnRetry && numTries > 1) {
          UserIO.getErr()
              .printf("Encountered error, retrying request (%s/%s)%n", numTries - 1, maxCalls);
        }

        T result = makeRequest.makeRequest();
        logger.debug("Result: {}", result);

        boolean jobCompleted = isDone.test(result);
        boolean timedOut = numTries > maxCalls;
        if (jobCompleted || timedOut) {
          // polling is either done (i.e. job completed) or timed out: return the last result
          logger.debug(
              "polling with retries completed. jobCompleted = {}, timedOut = {}",
              jobCompleted,
              timedOut);
          return result;
        }
      } catch (Exception ex) {
        if (!isRetryable.test(ex)) {
          // the exception is not retryable: re-throw
          throw ex;
        } else {
          // keep track of the last retryable exception so we can re-throw it in case of a timeout
          lastRetryableException = ex;
        }
        logger.info("Caught retryable exception: ", ex);
      }

      // sleep before retrying, unless this is the last try
      if (numTries < maxCalls) {
        Thread.sleep(sleepDuration.toMillis());
      }
    } while (numTries <= maxCalls);

    // request with retries timed out: re-throw the last exception
    throw new SystemException(
        "Request with retries timed out after " + numTries + " tries.", lastRetryableException);
  }

  /**
   * Helper method to make a request, handle a possible one-time error, and then retry the request.
   *
   * <p>Example: - Make a request to add a user to a workspace.
   *
   * <p>- Catch a user not found error. Handle it by making a request to invite the user.
   *
   * <p>- Retry the request to add a user to a workspace.
   *
   * @param makeRequest function to perform the request
   * @param makeRequestIsRetryable function to test whether an exception thrown by makeRequest is
   *     retryable or not
   * @param isOneTimeError function to test whether the exception is the expected one-time error
   * @param handleOneTimeError function to handle the one-time error before retrying the request
   * @param handleOneTimeErrorIsRetryable function to test whether an exception thrown by
   *     handleOneTimeError is retryable or not
   * @throws E1 if makeRequest throws an exception that is not the expected one-time error
   * @throws E2 if handleOneTimeError throws an exception
   */
  public static <E1 extends Exception, E2 extends Exception>
      void callAndHandleOneTimeErrorWithRetries(
          RunnableWithCheckedException<E1> makeRequest,
          Predicate<Exception> makeRequestIsRetryable,
          Predicate<Exception> isOneTimeError,
          RunnableWithCheckedException<E2> handleOneTimeError,
          Predicate<Exception> handleOneTimeErrorIsRetryable)
          throws E1, E2, InterruptedException {
    callAndHandleOneTimeErrorWithRetries(
        () -> {
          makeRequest.run();
          return null;
        },
        makeRequestIsRetryable,
        isOneTimeError,
        handleOneTimeError,
        handleOneTimeErrorIsRetryable);
  }

  /**
   * Helper method to make a request, handle a possible one-time error, and then retry the request.
   *
   * <p>Example: - Make a request to add a user to a workspace.
   *
   * <p>- Catch a user not found error. Handle it by making a request to invite the user.
   *
   * <p>- Retry the request to add a user to a workspace.
   *
   * @param makeRequest function to perform the request
   * @param makeRequestIsRetryable function to test whether an exception thrown by makeRequest is
   *     retryable or not
   * @param isOneTimeError function to test whether the exception is the expected one-time error
   * @param handleOneTimeError function to handle the one-time error before retrying the request
   * @param handleOneTimeErrorIsRetryable function to test whether an exception thrown by
   *     handleOneTimeError is retryable or not
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @return the response object
   * @throws E1 if makeRequest throws an exception that is not the expected one-time error
   * @throws E2 if handleOneTimeError throws an exception
   */
  public static <T, E1 extends Exception, E2 extends Exception>
      T callAndHandleOneTimeErrorWithRetries(
          SupplierWithCheckedException<T, E1> makeRequest,
          Predicate<Exception> makeRequestIsRetryable,
          Predicate<Exception> isOneTimeError,
          RunnableWithCheckedException<E2> handleOneTimeError,
          Predicate<Exception> handleOneTimeErrorIsRetryable)
          throws E1, E2, InterruptedException {
    try {
      // make the initial request
      return callWithRetries(makeRequest, makeRequestIsRetryable);
    } catch (Exception ex) {
      // if the exception is not the expected one-time error, then quit here
      if (!isOneTimeError.test(ex)) {
        throw ex;
      }
      logger.info("Caught possible one-time error: ", ex);

      // handle the one-time error
      callWithRetries(handleOneTimeError, handleOneTimeErrorIsRetryable);

      // retry the request. include some retries for the one-time error, to allow time for
      // information to propagate (this delay seems to happen on inviting a new user -- sometimes it
      // takes several seconds for WSM to recognize a newly invited user in SAM. not sure why)
      return callWithRetries(
          makeRequest, (ex2) -> isOneTimeError.test(ex2) || makeRequestIsRetryable.test(ex2));
    }
  }

  /**
   * Function interface for making a retryable Http request. This interface is explicitly defined so
   * that it can throw an exception (i.e. Supplier does not have this method annotation).
   *
   * @param <T> type of the Http response (i.e. return type of the makeRequest method)
   */
  @FunctionalInterface
  public interface SupplierWithCheckedException<T, E extends Exception> {
    T makeRequest() throws E, InterruptedException;
  }

  /**
   * Function interface for making a retryable Http request. This interface is explicitly defined so
   * that it can throw an exception (i.e. Runnable does not have this method annotation).
   */
  @FunctionalInterface
  public interface RunnableWithCheckedException<E extends Exception> {
    void run() throws E, InterruptedException;
  }

  /** This is a POJO class to hold the HTTP status code and the raw JSON response body. */
  public static class HttpResponse {
    public String responseBody;
    public int statusCode;

    HttpResponse(String responseBody, int statusCode) {
      this.responseBody = responseBody;
      this.statusCode = statusCode;
    }
  }
}
