package com.knowledgehub.shared.error;

import java.util.function.Supplier;

/**
 * Runs an MCP tool's work and normalises any failure into the same {@code code}/{@code message}
 * contract REST uses, so an agent branches on identical error codes at either boundary. Domain
 * failures keep their code; a rejected argument maps to {@code VALIDATION_FAILED}; anything else is
 * an {@code INTERNAL_ERROR} with no internal detail leaked. The MCP server surfaces the thrown
 * {@link ToolFailure} message to the model as the tool error.
 */
public final class ToolErrors {

  private ToolErrors() {}

  /** Invokes {@code action}, translating any failure into a {@link ToolFailure}. */
  public static <T> T mapped(Supplier<T> action) {
    try {
      return action.get();
    } catch (DomainException e) {
      throw new ToolFailure(e.errorCode(), e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ToolFailure(ErrorCode.VALIDATION_FAILED, e.getMessage());
    } catch (RuntimeException e) {
      throw new ToolFailure(ErrorCode.INTERNAL_ERROR, null);
    }
  }
}
