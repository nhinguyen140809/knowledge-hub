package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.knowledgehub.knowledge.ingestion.application.SourceSpec;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request body to register a source. {@code ref} is optional (Git only); {@code include}/{@code
 * ignore} default to empty when omitted.
 */
public record CreateSourceRequest(
    @NotBlank String id,
    @NotNull SourceType type,
    @NotBlank String uriOrPath,
    String ref,
    List<String> include,
    List<String> ignore) {

  SourceSpec toSpec() {
    return new SourceSpec(
        id,
        type,
        uriOrPath,
        ref,
        include == null ? List.of() : include,
        ignore == null ? List.of() : ignore);
  }
}
