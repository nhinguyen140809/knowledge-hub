package com.knowledgehub.shared.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Single place that turns exceptions into RFC 7807 {@link ProblemDetail} responses, so every REST
 * error shares one shape: {@code { type, title, status, detail, code, traceId }}. The {@code code}
 * comes from {@link ErrorCode}; MCP tools reuse the same code/message at their boundary.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Business failures carry their own stable {@link ErrorCode}. */
  @ExceptionHandler(DomainException.class)
  public ProblemDetail handleDomain(DomainException ex) {
    log.debug("Domain error {}: {}", ex.errorCode(), ex.getMessage());
    return problem(ex.errorCode(), ex.getMessage());
  }

  /** Bean Validation failures on {@code @Valid} request bodies. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse(ErrorCode.VALIDATION_FAILED.defaultMessage());
    return problem(ErrorCode.VALIDATION_FAILED, detail);
  }

  /** Invalid arguments (e.g. a domain invariant rejected the input) map to a 400. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    return problem(ErrorCode.VALIDATION_FAILED, ex.getMessage());
  }

  /** Anything unmapped is an internal error; log the cause, never leak it to the client. */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return problem(ErrorCode.INTERNAL_ERROR, null);
  }

  private static ProblemDetail problem(ErrorCode code, String detail) {
    ProblemDetail pd = ProblemDetail.forStatus(code.status());
    pd.setTitle(code.status().getReasonPhrase());
    pd.setDetail(detail != null ? detail : code.defaultMessage());
    pd.setProperty("code", code.name());
    String traceId = MDC.get("traceId");
    if (traceId != null) {
      pd.setProperty("traceId", traceId);
    }
    return pd;
  }
}
