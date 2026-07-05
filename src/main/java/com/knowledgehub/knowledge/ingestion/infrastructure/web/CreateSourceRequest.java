package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.knowledgehub.knowledge.ingestion.application.SourceSpec;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body to register a source. {@code ref} is optional (Git only); {@code include}/{@code
 * ignore} default to empty when omitted; {@code name} and {@code description} are optional
 * human-facing metadata (a short label and a longer explanation).
 */
public record CreateSourceRequest(
    @NotBlank String id,
    @NotNull SourceType type,
    @NotBlank String uriOrPath,
    String ref,
    List<String> include,
    List<String> ignore,
    @Size(max = 200) String name,
    @Size(max = 2000) String description) {

  SourceSpec toSpec() {
    return new SourceSpec(
        id,
        type,
        uriOrPath,
        ref,
        include == null ? List.of() : include,
        ignore == null ? List.of() : ignore,
        name,
        description);
  }
}
