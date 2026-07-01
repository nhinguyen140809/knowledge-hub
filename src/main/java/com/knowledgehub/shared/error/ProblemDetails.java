package com.knowledgehub.shared.error;

import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

/**
 * Builds the application's single RFC 7807 {@link ProblemDetail} shape — {@code { type, title,
 * status, detail, code, traceId }} — so every error boundary (the controller advice and the
 * security filter chain alike) produces the same body.
 */
public final class ProblemDetails {

  private ProblemDetails() {}

  /**
   * A problem detail for an {@link ErrorCode}, using {@code detail} or the code's default message.
   */
  public static ProblemDetail of(ErrorCode code, String detail) {
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
