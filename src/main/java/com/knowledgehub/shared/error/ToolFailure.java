package com.knowledgehub.shared.error;

/**
 * A failure surfaced from an MCP tool, carrying the same stable {@link ErrorCode} the REST boundary
 * uses. The message is {@code "[CODE] detail"} so an agent reading the tool error branches on the
 * exact same codes it would see in a {@code ProblemDetail}. Thrown by {@link ToolErrors}.
 */
public class ToolFailure extends RuntimeException {

  private final transient ErrorCode errorCode;

  ToolFailure(ErrorCode errorCode, String detail) {
    super("[" + errorCode.name() + "] " + (detail != null ? detail : errorCode.defaultMessage()));
    this.errorCode = errorCode;
  }

  /** The stable error code carried by this failure. */
  public ErrorCode errorCode() {
    return errorCode;
  }
}
