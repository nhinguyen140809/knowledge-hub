package com.knowledgehub.knowledge.ingestion.infrastructure.mcp;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;

/**
 * One source the calling agent may read: enough to recognize it and target the other tools — the id
 * to pass them, the kind, the configured Git ref ({@code null} for a filesystem source or a Git
 * source on its default branch), and the human-facing {@code name}/{@code description} that say
 * what the source holds ({@code null} when unset). Deliberately omits the source's location and
 * glob configuration, which are administrative detail.
 */
public record ReadableSourceResponse(
    String sourceId, SourceType type, String ref, String name, String description) {

  static ReadableSourceResponse from(Source source) {
    return new ReadableSourceResponse(
        source.sourceId(),
        source.type(),
        source.ref().orElse(null),
        source.name().orElse(null),
        source.description().orElse(null));
  }
}
