package com.knowledgehub.access.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgehub.shared.error.ErrorCode;
import com.knowledgehub.shared.error.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Renders the security filter chain's 401 (no/invalid token) and 403 (authenticated but not admin)
 * as the same {@link ProblemDetail} body the controller advice uses, so an agent sees one error
 * shape everywhere. Security failures happen before any controller runs, so without this the
 * default Spring pages would leak a second, different error format.
 *
 * <p>Example body for a request without a usable token:
 *
 * <pre>{@code
 * { "title": "Unauthorized", "status": 401,
 *   "detail": "Authentication required or invalid", "code": "UNAUTHENTICATED" }
 * }</pre>
 */
@Component
class ProblemSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  ProblemSecurityHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Answers a request that reached a protected endpoint without usable authentication — no token,
   * an unknown secret, or a revoked credential — with a 401 problem-detail body.
   */
  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    write(response, ErrorCode.UNAUTHENTICATED);
  }

  /**
   * Answers an authenticated request that lacks the required role — a member calling an admin-only
   * endpoint — with a 403 problem-detail body.
   */
  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    write(response, ErrorCode.FORBIDDEN);
  }

  /** Writes the error code's problem detail as the {@code application/problem+json} response. */
  private void write(HttpServletResponse response, ErrorCode code) throws IOException {
    ProblemDetail problem = ProblemDetails.of(code, null);
    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
