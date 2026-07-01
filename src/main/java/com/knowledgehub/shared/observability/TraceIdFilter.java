package com.knowledgehub.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stamps every request with a correlation id so a single request can be reconstructed from the
 * logs. The id is taken from the inbound {@code X-Trace-Id} header when a caller supplies one
 * (letting an agent correlate across the MCP boundary) and generated otherwise; it lives in the
 * logging {@link MDC} for the duration of the request and is echoed back in the response header.
 *
 * <p>Runs first, before the security chain, so even an authentication or authorization failure is
 * logged and returned with a trace id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  static final String TRACE_ID = "traceId";
  static final String HEADER = "X-Trace-Id";

  // An inbound id is honoured only if it is safe to write into a log line; anything else (control
  // characters, over-long values) is replaced with a generated id to prevent log forging.
  private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = resolve(request.getHeader(HEADER));
    MDC.put(TRACE_ID, traceId);
    response.setHeader(HEADER, traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(TRACE_ID);
    }
  }

  private static String resolve(String inbound) {
    if (inbound != null && SAFE_TRACE_ID.matcher(inbound.trim()).matches()) {
      return inbound.trim();
    }
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
