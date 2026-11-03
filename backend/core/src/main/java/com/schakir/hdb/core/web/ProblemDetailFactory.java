package com.schakir.hdb.core.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

/**
 * Builds every {@code application/problem+json} response (RFC 9457, D-021). Each module maps its
 * own business problems through {@link #response}; the security filter chain (#6) reuses {@link
 * #writeTo} for 401/403, written outside MVC dispatch (ADR 0037).
 *
 * <p>Build every problem response with this factory, never by returning a {@link ProblemDetail}
 * from {@code ResponseEntity.body(...)} directly: Spring MVC fills a null {@code instance} with the
 * request URI otherwise, which must never happen here (correlation ids are #13, not yet
 * implemented).
 */
public final class ProblemDetailFactory {

  private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
  private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

  private ProblemDetailFactory() {}

  /** A business or validation problem: {@code type: /problems/<kebab-case-name>}. */
  public static ResponseEntity<Object> response(
      HttpStatusCode status, String problemName, String title, String detail) {
    ProblemDetail problem = buildProblem(status, problemName, title, detail);
    return toResponseEntity(problem, new HttpHeaders());
  }

  /**
   * Writes a business problem directly to a servlet response, for the one place outside MVC
   * dispatch that needs one: the security filter chain's 401/403 (ADR 0037). {@code mapper} must be
   * the application's own, configured bean (snake_case, {@code non_null}), not a fresh instance.
   *
   * <p>Never calls {@code response.reset()}: the caller (a {@code Bearer*} entry
   * point/access-denied handler) has already set the status and the {@code WWW-Authenticate}
   * header, and resetting would erase it.
   */
  public static void writeTo(
      HttpServletResponse response,
      ObjectMapper mapper,
      HttpStatusCode status,
      String problemName,
      String title,
      String detail)
      throws IOException {
    ProblemDetail problem = buildProblem(status, problemName, title, detail);
    setInstance(problem);
    response.setStatus(problem.getStatus());
    response.setContentType(PROBLEM_JSON.toString());
    mapper.writeValue(response.getOutputStream(), asResponseBody(problem));
  }

  private static ProblemDetail buildProblem(
      HttpStatusCode status, String problemName, String title, String detail) {
    if (!KEBAB_CASE.matcher(problemName).matches()) {
      throw new IllegalArgumentException("Problem name must be kebab-case: " + problemName);
    }
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setType(URI.create("/problems/" + problemName));
    problem.setTitle(title);
    if (detail != null) {
      problem.setDetail(detail);
    }
    return problem;
  }

  /** An unclassified problem: {@code type: about:blank} (RFC 9457's own fallback). */
  public static ResponseEntity<Object> aboutBlank(
      HttpStatusCode status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setType(URI.create("about:blank"));
    problem.setTitle(title);
    if (detail != null) {
      problem.setDetail(detail);
    }
    return toResponseEntity(problem, new HttpHeaders());
  }

  /**
   * Converts a {@link ProblemDetail} that may still be missing its {@code type} (as built by {@code
   * ResponseEntityExceptionHandler}'s own default handling, for an exception this module does not
   * override) into the final response: {@code type} defaults to {@code /problems/validation-error}
   * for a 400, {@code about:blank} otherwise; {@code instance} is set from the MDC correlation id,
   * never the request URI. {@code headers} is carried through unchanged except for the content type
   * (e.g. a 405's {@code Allow} or a 415's {@code Accept}, which {@code
   * ResponseEntityExceptionHandler} already computed and which this class must not drop).
   */
  static ResponseEntity<Object> toResponseEntity(ProblemDetail problem, HttpHeaders headers) {
    if (problem.getType() == null) {
      if (problem.getStatus() == HttpStatus.BAD_REQUEST.value()) {
        problem.setType(URI.create("/problems/validation-error"));
        problem.setTitle("Invalid request");
      } else {
        problem.setType(URI.create("about:blank"));
      }
    }
    setInstance(problem);
    HttpHeaders responseHeaders = HttpHeaders.copyOf(headers);
    responseHeaders.setContentType(PROBLEM_JSON);
    return ResponseEntity.status(problem.getStatus())
        .headers(responseHeaders)
        .body(asResponseBody(problem));
  }

  private static void setInstance(ProblemDetail problem) {
    String correlationId = MDC.get(CorrelationId.MDC_KEY);
    problem.setInstance(
        correlationId != null && !correlationId.isBlank() ? URI.create(correlationId) : null);
  }

  private static Map<String, Object> asResponseBody(ProblemDetail problem) {
    Map<String, Object> body = new LinkedHashMap<>();
    putIfPresent(body, "type", problem.getType());
    putIfPresent(body, "title", problem.getTitle());
    body.put("status", problem.getStatus());
    putIfPresent(body, "detail", problem.getDetail());
    putIfPresent(body, "instance", problem.getInstance());
    return body;
  }

  private static void putIfPresent(Map<String, Object> body, String key, Object value) {
    if (value != null) {
      body.put(key, value);
    }
  }
}
