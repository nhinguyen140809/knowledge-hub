package com.knowledgehub.knowledge.ingestion.infrastructure.mcp;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;

/**
 * One source the calling agent may read: just enough to target the other tools — the id to pass
 * them, the kind, and the configured Git ref ({@code null} for a filesystem source or a Git source
 * on its default branch). Deliberately omits the source's location and glob configuration, which
 * are administrative detail.
 */
public record ReadableSourceResponse(String sourceId, SourceType type, String ref) {

  static ReadableSourceResponse from(Source source) {
    return new ReadableSourceResponse(source.sourceId(), source.type(), source.ref().orElse(null));
  }
}
