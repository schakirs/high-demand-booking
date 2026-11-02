package com.schakir.hdb.core.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Builds {@code application/problem+json} bodies with {@code type: /problems/<kebab-case-name>}
 * (RFC 9457, D-021). Each module maps its own business problems through this factory; the security
 * filter chain (#6) will reuse it for 401/403 (ADR 0037).
 *
 * <p>{@link #asResponseBody} must be used to turn the result into the actual response body, instead
 * of handing the {@link ProblemDetail} itself to {@code ResponseEntity.body(...)}: Spring MVC's
 * {@code HttpEntityMethodProcessor} auto-fills {@code instance} from the request URI whenever a
 * {@code ResponseEntity<ProblemDetail>} body has a null {@code instance}, which this API must never
 * send when no correlation id is set (#13).
 */
public final class ProblemDetailFactory {

  private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

  private ProblemDetailFactory() {}

  public static ProblemDetail forProblem(
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
    setInstance(problem);
    return problem;
  }

  /** Sets {@code instance} from the correlation id in the MDC, when present (#13). */
  public static void setInstance(ProblemDetail problem) {
    String correlationId = MDC.get(CorrelationId.MDC_KEY);
    if (correlationId != null && !correlationId.isBlank()) {
      problem.setInstance(URI.create(correlationId));
    }
  }

  /**
   * The actual response body: a plain, ordered map with absent fields omitted, never a JSON {@code
   * null} (see the class Javadoc for why {@code problem} itself must not be returned).
   */
  public static Map<String, Object> asResponseBody(ProblemDetail problem) {
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
