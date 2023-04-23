package bio.terra.cli.service.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.ControlledResourceCommonFields;
import bio.terra.workspace.model.ErrorReport;
import bio.terra.workspace.model.JobReport;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.Property;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.SocketException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager endpoints. */
public class WsmUtils {
  private static final Logger logger = LoggerFactory.getLogger(WsmUtils.class);

  /**
   * Helper method to convert a local date (e.g. 2014-01-02) into an object that includes time and
   * zone. The time is set to midnight, the zone to UTC.
   *
   * @param localDate date object with no time or zone/offset information included
   * @return object that specifies the date, time and zone/offest
   */
  public static OffsetDateTime dateAtMidnightAndUTC(@Nullable LocalDate localDate) {
    return localDate == null
        ? null
        : OffsetDateTime.of(localDate.atTime(LocalTime.MIDNIGHT), ZoneOffset.UTC);
  }

  /**
   * Create a common fields WSM object from a Resource that is being used to create a controlled
   * resource.
   */
  public static ControlledResourceCommonFields createCommonFields(
      CreateResourceParams createParams) {
    return new ControlledResourceCommonFields()
        .name(createParams.name)
        .description(createParams.description)
        .cloningInstructions(createParams.cloningInstructions)
        .accessScope(createParams.accessScope)
        .managedBy(ManagedBy.USER);
  }

  /** Helper method that checks a JobReport's status and returns false if it's still RUNNING. */
  public static boolean isDone(JobReport jobReport) {
    return !jobReport.getStatus().equals(JobReport.StatusEnum.RUNNING);
  }

  /**
   * Helper method that checks a JobReport's status and throws an exception if it's not COMPLETED.
   *
   * <p>- Throws a {@link SystemException} if the job FAILED.
   *
   * <p>- Throws a {@link UserActionableException} if the job is still RUNNING. Some actions are
   * expected to take a long time (e.g. deleting a bucket with lots of objects), and a timeout is
   * not necessarily a failure. The action the user can take is to wait a bit longer and then check
   * back (e.g. by listing the buckets in the workspace) later to see if the job completed.
   *
   * @param jobReport WSM job report object
   * @param errorReport WSM error report object
   */
  public static void throwIfJobNotCompleted(JobReport jobReport, ErrorReport errorReport) {
    switch (jobReport.getStatus()) {
      case FAILED -> throw new SystemException("Job failed: " + errorReport.getMessage());
      case RUNNING -> throw new UserActionableException(
          "CLI timed out waiting for the job to complete. It's still running on the server.");
    }
  }

  /**
   * Utility method that checks if an exception thrown by the WSM client matches the given HTTP
   * status code.
   *
   * @param ex exception to test
   * @return true if the exception status code matches
   */
  public static boolean isHttpStatusCode(Exception ex, int statusCode) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    int exceptionStatusCode = ((ApiException) ex).getCode();
    return statusCode == exceptionStatusCode;
  }

  /**
   * Utility method that checks if an exception thrown by the WSM client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  public static boolean isRetryable(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    logErrorMessage((ApiException) ex);
    int statusCode = ((ApiException) ex).getCode();
    // if a request to WSM times out, the client will wrap a SocketException in an ApiException,
    // set the HTTP status code to 0, and rethrows it to the caller. Unfortunately this is a
    // different exception than the SocketTimeoutException thrown by other client libraries.
    final int TIMEOUT_STATUS_CODE = 0;
    boolean isWsmTimeout =
        statusCode == TIMEOUT_STATUS_CODE && ex.getCause() instanceof SocketException;

    return isWsmTimeout
        || statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT;
  }

  /** Pull a human-readable error message from an ApiException. */
  public static String logErrorMessage(ApiException apiEx) {
    logger.error(
        "WSM exception status code: {}, response body: {}, message: {}",
        apiEx.getCode(),
        apiEx.getResponseBody(),
        apiEx.getMessage());

    // try to deserialize the response body into an ErrorReport
    String apiExMsg = apiEx.getResponseBody();
    if (apiExMsg != null)
      try {
        ErrorReport errorReport =
            JacksonMapper.getMapper().readValue(apiEx.getResponseBody(), ErrorReport.class);
        apiExMsg = errorReport.getMessage();
      } catch (JsonProcessingException jsonEx) {
        logger.debug("Error deserializing WSM exception ErrorReport: {}", apiEx.getResponseBody());
      }

    // if we found a SAM error message, then return it
    // otherwise return a string with the http code
    return ((apiExMsg != null && !apiExMsg.isEmpty())
        ? apiExMsg
        : apiEx.getCode() + " " + apiEx.getMessage());
  }

  /**
   * Execute a function that includes hitting WSM endpoints. If an exception is thrown by the WSM
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (ApiException | InterruptedException ex) {
      // if this is a WSM client exception, check for a message in the response body
      if (ex instanceof ApiException) {
        String exceptionErrorMessage = logErrorMessage((ApiException) ex);

        errorMsg += ": " + exceptionErrorMessage;
      }

      // wrap the WSM exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }

  public static List<Property> buildProperties(@Nullable Map<String, String> propertyMap) {
    if (propertyMap == null) {
      return new ArrayList<>();
    }
    return propertyMap.entrySet().stream()
        .map(
            entry -> {
              Property property = new Property();
              property.setKey(entry.getKey());
              property.setValue(entry.getValue());
              return property;
            })
        .collect(Collectors.toList());
  }
}
