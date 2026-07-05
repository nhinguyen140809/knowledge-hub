package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for a partial update of a source's editable fields. Merge semantics per field: an
 * omitted field (JSON {@code null}) keeps its current value, an empty array {@code []} clears that
 * glob list and a non-empty array replaces it, and a blank {@code name}/{@code description} clears
 * it. {@code ref} is valid for Git sources only; id, type, and location are fixed and cannot be
 * changed here. At least one field must be present — an empty request has nothing to change and is
 * rejected.
 */
public record UpdateSourceRequest(
    String ref,
    List<String> include,
    List<String> ignore,
    @Size(max = 200) String name,
    @Size(max = 2000) String description) {

  @JsonIgnore
  @AssertTrue(message = "at least one of ref, include, ignore, name, description must be provided")
  boolean isNonEmpty() {
    return ref != null || include != null || ignore != null || name != null || description != null;
  }
}
