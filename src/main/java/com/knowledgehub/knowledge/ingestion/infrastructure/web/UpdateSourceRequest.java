package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;

/**
 * Request body for a partial update of a source's editable configuration. Merge semantics per
 * field: an omitted field (JSON {@code null}) keeps its current value, an empty array {@code []}
 * clears that glob list, and a non-empty array replaces it. {@code ref} is valid for Git sources
 * only; id, type, and location are fixed and cannot be changed here. At least one field must be
 * present — an empty request has nothing to change and is rejected.
 */
public record UpdateSourceRequest(String ref, List<String> include, List<String> ignore) {

  @JsonIgnore
  @AssertTrue(message = "at least one of ref, include, ignore must be provided")
  boolean isNonEmpty() {
    return ref != null || include != null || ignore != null;
  }
}
