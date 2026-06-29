package com.knowledgehub.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTests {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsDomainExceptionToStatusCodeAndDetail() {
    ProblemDetail pd =
        handler.handleDomain(new DomainException(ErrorCode.SOURCE_NOT_FOUND, "source x missing"));

    assertThat(pd.getStatus()).isEqualTo(404);
    assertThat(pd.getDetail()).isEqualTo("source x missing");
    assertThat(pd.getProperties()).containsEntry("code", "SOURCE_NOT_FOUND");
  }

  @Test
  void fallsBackToTheCodeDefaultMessageWhenNoDetail() {
    ProblemDetail pd = handler.handleDomain(new DomainException(ErrorCode.DUPLICATE_SOURCE));

    assertThat(pd.getStatus()).isEqualTo(409);
    assertThat(pd.getDetail()).isEqualTo("Source already exists");
    assertThat(pd.getProperties()).containsEntry("code", "DUPLICATE_SOURCE");
  }

  @Test
  void illegalArgumentBecomesValidationError() {
    ProblemDetail pd =
        handler.handleIllegalArgument(new IllegalArgumentException("ref only for GIT"));

    assertThat(pd.getStatus()).isEqualTo(400);
    assertThat(pd.getProperties()).containsEntry("code", "VALIDATION_FAILED");
    assertThat(pd.getDetail()).isEqualTo("ref only for GIT");
  }

  @Test
  void unexpectedExceptionBecomesInternalErrorWithoutLeakingTheCause() {
    ProblemDetail pd = handler.handleUnexpected(new RuntimeException("DB password = s3cret"));

    assertThat(pd.getStatus()).isEqualTo(500);
    assertThat(pd.getProperties()).containsEntry("code", "INTERNAL_ERROR");
    assertThat(pd.getDetail()).isEqualTo("Unexpected error").doesNotContain("s3cret");
  }
}
