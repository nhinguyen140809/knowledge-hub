package com.knowledgehub.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Stable, app-defined API error codes — the single source of truth for the {@code code} field in
 * every {@link org.springframework.http.ProblemDetail} response. Each value pairs a code name with
 * its HTTP status and a default human-readable message.
 *
 * <p>The code <strong>names are a contract</strong> that REST and MCP clients (AI agents) branch
 * on, so renaming a value is a breaking change. Messages live here, not in a {@code MessageSource}
 * — the service is internal/agent-facing and has no i18n requirement.
 */
public enum ErrorCode {
  SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Source not found"),
  DUPLICATE_SOURCE(HttpStatus.CONFLICT, "Source already exists"),
  PRINCIPAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Principal not found"),
  DUPLICATE_PRINCIPAL(HttpStatus.CONFLICT, "Principal already exists"),
  DUPLICATE_CREDENTIAL_NAME(
      HttpStatus.CONFLICT, "Credential name already in use for this principal"),
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required or invalid"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Operation not permitted"),
  EMBEDDING_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Embedding provider unavailable"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

  private final HttpStatus status;
  private final String defaultMessage;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  /** The HTTP status this error maps to. */
  public HttpStatus status() {
    return status;
  }

  /** The default message used when a thrower supplies no specific detail. */
  public String defaultMessage() {
    return defaultMessage;
  }
}
