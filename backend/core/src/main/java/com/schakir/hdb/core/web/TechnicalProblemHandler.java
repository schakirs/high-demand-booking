package com.schakir.hdb.core.web;

import java.net.URI;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Technical error handling shared by every API module (ADR 0037): validation failures and malformed
 * requests become {@code 400 /problems/validation-error}; any unhandled exception becomes a {@code
 * 500 about:blank} with no technical detail in the body (it is logged at ERROR with its stack trace
 * instead); other undeclared Spring MVC errors keep their status with {@code about:blank}. Lowest
 * precedence, so a module's own {@code @RestControllerAdvice} for its business problems is tried
 * first.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class TechnicalProblemHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(TechnicalProblemHandler.class);
  private static final Pattern CAMEL_HUMP = Pattern.compile("([a-z0-9])([A-Z])");
  private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(TechnicalProblemHandler::describeFieldError)
            .collect(Collectors.joining("; "));
    return validationError(detail);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return validationError(describeUnreadableBody(ex));
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String property = ex.getPropertyName();
    String detail =
        property == null
            ? "A path parameter must be a valid UUID."
            : property + ": must be a valid UUID";
    return validationError(detail);
  }

  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    return aboutBlank(status);
  }

  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return aboutBlank(status);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return aboutBlank(status);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return aboutBlank(status);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    ProblemDetail problem =
        ProblemDetail.forStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    problem.setType(URI.create("about:blank"));
    problem.setTitle("Internal server error");
    problem.setDetail("An unexpected error occurred.");
    ProblemDetailFactory.setInstance(problem);
    return ResponseEntity.status(problem.getStatus())
        .contentType(PROBLEM_JSON)
        .body(ProblemDetailFactory.asResponseBody(problem));
  }

  private ResponseEntity<Object> validationError(String detail) {
    ProblemDetail problem =
        ProblemDetailFactory.forProblem(
            org.springframework.http.HttpStatus.BAD_REQUEST,
            "validation-error",
            "Invalid request",
            detail);
    return ResponseEntity.status(problem.getStatus())
        .contentType(PROBLEM_JSON)
        .body(ProblemDetailFactory.asResponseBody(problem));
  }

  private ResponseEntity<Object> aboutBlank(HttpStatusCode status) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setType(URI.create("about:blank"));
    ProblemDetailFactory.setInstance(problem);
    return ResponseEntity.status(status)
        .contentType(PROBLEM_JSON)
        .body(ProblemDetailFactory.asResponseBody(problem));
  }

  private static String describeFieldError(FieldError error) {
    return toSnakeCase(error.getField()) + ": " + error.getDefaultMessage();
  }

  private static String describeUnreadableBody(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getMostSpecificCause();
    if (cause != null && cause.getClass().getSimpleName().equals("UnrecognizedPropertyException")) {
      String propertyName = extractUnrecognizedPropertyName(cause);
      return propertyName == null
          ? "The request body contains an unknown property."
          : propertyName + ": unknown property";
    }
    return "The request body is malformed or missing.";
  }

  private static String extractUnrecognizedPropertyName(Throwable cause) {
    try {
      Object propertyName = cause.getClass().getMethod("getPropertyName").invoke(cause);
      return propertyName == null ? null : propertyName.toString();
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static String toSnakeCase(String camelCase) {
    return CAMEL_HUMP.matcher(camelCase).replaceAll("$1_$2").toLowerCase();
  }
}
