package com.knowledgehub.knowledge.ingestion.application;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;

/**
 * Command describing a source to register. Carries the raw inputs; {@link #toSource()} builds the
 * domain {@link Source}, which enforces the invariants (e.g. {@code ref} only for Git).
 */
public record SourceSpec(
    String id,
    SourceType type,
    String uriOrPath,
    String ref,
    List<String> include,
    List<String> ignore,
    String name,
    String description) {

  /** Builds the domain entity, applying its construction-time invariants. */
  public Source toSource() {
    return new Source(id, type, uriOrPath, ref, include, ignore, name, description);
  }
}
