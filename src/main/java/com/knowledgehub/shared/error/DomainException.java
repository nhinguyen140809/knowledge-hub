package com.knowledgehub.shared.error;

/**
 * Base class for business failures. Carries a stable {@link ErrorCode}; the central {@link
 * GlobalExceptionHandler} maps it to an HTTP {@code ProblemDetail}, so no controller writes its own
 * HTTP-mapping try/catch (FR-5.4).
 *
 * <p>Features extend this with specific types (e.g. {@code SourceNotFoundException}) in their own
 * {@code domain} package; the type is just a thin wrapper choosing the {@link ErrorCode}.
 */
public class DomainException extends RuntimeException {

  private final transient ErrorCode errorCode;

  /**
   * @param errorCode the stable error code (also determines HTTP status)
   * @param detail a specific human-readable detail, or {@code null} to use the code's default
   */
  public DomainException(ErrorCode errorCode, String detail) {
    super(detail != null ? detail : errorCode.defaultMessage());
    this.errorCode = errorCode;
  }

  /** Uses the error code's default message as the detail. */
  public DomainException(ErrorCode errorCode) {
    this(errorCode, null);
  }

  /** The stable error code carried by this exception. */
  public ErrorCode errorCode() {
    return errorCode;
  }
}
