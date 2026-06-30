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
 * shape everywhere.
 */
@Component
class ProblemSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  ProblemSecurityHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    write(response, ErrorCode.UNAUTHENTICATED);
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    write(response, ErrorCode.FORBIDDEN);
  }

  private void write(HttpServletResponse response, ErrorCode code) throws IOException {
    ProblemDetail problem = ProblemDetails.of(code, null);
    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
