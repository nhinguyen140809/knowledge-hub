package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import java.util.List;

/**
 * Request body to update a source's editable configuration. Replaces the current {@code ref} and
 * the include/ignore globs (PUT semantics: an omitted list clears to empty). {@code ref} is valid
 * for Git sources only; id, type, and location are fixed and cannot be changed here.
 */
public record UpdateSourceRequest(String ref, List<String> include, List<String> ignore) {}
