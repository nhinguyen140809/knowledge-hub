package com.knowledgehub.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ToolErrorsTests {

  @Test
  void passesThroughASuccessfulResult() {
    assertThat(ToolErrors.mapped(() -> "ok")).isEqualTo("ok");
  }

  @Test
  void mapsADeniedAuthorizationToForbidden() {
    assertThatThrownBy(() -> ToolErrors.mapped(failWith(new AccessDeniedException("no"))))
        .isInstanceOf(ToolFailure.class)
        .satisfies(e -> assertThat(((ToolFailure) e).errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
  }

  @Test
  void mapsAnUnexpectedFailureToAnOpaqueInternalError() {
    assertThatThrownBy(() -> ToolErrors.mapped(failWith(new IllegalStateException("boom"))))
        .isInstanceOf(ToolFailure.class)
        .satisfies(
            e -> assertThat(((ToolFailure) e).errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
        // The internal detail is never surfaced to the agent.
        .hasMessageNotContaining("boom");
  }

  private static java.util.function.Supplier<Object> failWith(RuntimeException e) {
    return () -> {
      throw e;
    };
  }
}
