package com.schakir.hdb.core.web;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Technical error handling shared by every API module (ADR 0037): validation failures and malformed
 * requests become {@code 400 /problems/validation-error}; any unhandled exception becomes a {@code
 * 500 about:blank} with no technical detail in the body (it is logged at ERROR with its stack trace
 * instead). Lowest precedence, so a module's own {@code @RestControllerAdvice} for its business
 * problems is tried first.
 *
 * <p>{@link #createResponseEntity} is the single choke point every response coming out of {@link
 * ResponseEntityExceptionHandler}'s own default handling passes through — including the exceptions
 * this class does not explicitly override (a missing request parameter, a raw {@code
 * ResponseStatusException}, …) — so none of them can bypass {@link ProblemDetailFactory}.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class TechnicalProblemHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(TechnicalProblemHandler.class);
  private static final Pattern CAMEL_HUMP = Pattern.compile("([a-z0-9])([A-Z])");

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail problem) {
      return ProblemDetailFactory.toResponseEntity(problem);
    }
    return super.createResponseEntity(body, headers, statusCode, request);
  }

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
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String detail =
        ex.getParameterValidationResults().stream()
            .map(TechnicalProblemHandler::describeParameterResult)
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
    Class<?> requiredType = ex.getRequiredType();
    String typeName = requiredType != null ? requiredType.getSimpleName() : "the expected type";
    String detail =
        property == null
            ? "A path parameter must be a valid " + typeName + "."
            : property + ": must be a valid " + typeName;
    return validationError(detail);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
    String detail =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));
    return validationError(detail);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ProblemDetailFactory.aboutBlank(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred.");
  }

  private static ResponseEntity<Object> validationError(String detail) {
    return ProblemDetailFactory.response(
        HttpStatus.BAD_REQUEST, "validation-error", "Invalid request", detail);
  }

  private static String describeFieldError(FieldError error) {
    return toSnakeCase(error.getField()) + ": " + error.getDefaultMessage();
  }

  private static String describeParameterResult(
      org.springframework.validation.method.ParameterValidationResult result) {
    String name = result.getMethodParameter().getParameterName();
    String messages =
        result.getResolvableErrors().stream()
            .map(error -> error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return (name == null ? "parameter" : name) + ": " + messages;
  }

  private static String describeUnreadableBody(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getMostSpecificCause();
    if (cause instanceof UnrecognizedPropertyException unrecognized) {
      return unrecognized.getPropertyName() + ": unknown property";
    }
    if (cause instanceof MismatchedInputException mismatched) {
      String field = lastFieldName(mismatched.getPath());
      String typeName =
          mismatched.getTargetType() != null
              ? mismatched.getTargetType().getSimpleName()
              : "the expected type";
      return field == null
          ? "The request body contains a value of the wrong type."
          : field + ": must be a valid " + typeName;
    }
    return "The request body is malformed or missing.";
  }

  private static String lastFieldName(List<JacksonException.Reference> path) {
    if (path.isEmpty()) {
      return null;
    }
    return path.get(path.size() - 1).getPropertyName();
  }

  private static String toSnakeCase(String camelCase) {
    return CAMEL_HUMP.matcher(camelCase).replaceAll("$1_$2").toLowerCase();
  }
}
