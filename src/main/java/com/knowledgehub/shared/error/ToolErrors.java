package com.knowledgehub.shared.error;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

/**
 * Runs an MCP tool's work and normalises any failure into the same {@code code}/{@code message}
 * contract REST uses, so an agent branches on identical error codes at either boundary. Domain
 * failures keep their code; a denied authorization maps to {@code FORBIDDEN}; a rejected argument
 * maps to {@code VALIDATION_FAILED}; anything else is an {@code INTERNAL_ERROR} with no internal
 * detail leaked. The MCP server surfaces the thrown {@link ToolFailure} message to the model as the
 * tool error.
 */
public final class ToolErrors {

  private static final Logger log = LoggerFactory.getLogger(ToolErrors.class);

  private ToolErrors() {}

  /** Invokes {@code action}, translating any failure into a {@link ToolFailure}. */
  public static <T> T mapped(Supplier<T> action) {
    try {
      return action.get();
    } catch (DomainException e) {
      throw new ToolFailure(e.errorCode(), e.getMessage());
    } catch (AccessDeniedException e) {
      throw new ToolFailure(ErrorCode.FORBIDDEN, null);
    } catch (IllegalArgumentException e) {
      throw new ToolFailure(ErrorCode.VALIDATION_FAILED, e.getMessage());
    } catch (RuntimeException e) {
      // Unlike the REST advice, an MCP tool has no central handler to log a surprise failure, so do
      // it here before returning the opaque INTERNAL_ERROR to the agent.
      log.error("MCP tool failed unexpectedly", e);
      throw new ToolFailure(ErrorCode.INTERNAL_ERROR, null);
    }
  }
}
