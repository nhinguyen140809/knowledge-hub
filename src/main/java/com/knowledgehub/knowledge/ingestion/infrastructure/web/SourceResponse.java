package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.time.Instant;
import java.util.List;

/**
 * Response view of a configured source. {@code updatedAt} is the moment the source was last synced
 * (ISO-8601), or {@code null} when it has never been synced.
 */
public record SourceResponse(
    String id,
    SourceType type,
    String uriOrPath,
    String ref,
    List<String> include,
    List<String> ignore,
    String name,
    String description,
    String updatedAt) {

  static SourceResponse from(Source source, Instant updatedAt) {
    return new SourceResponse(
        source.sourceId(),
        source.type(),
        source.uriOrPath(),
        source.ref().orElse(null),
        source.include(),
        source.ignore(),
        source.name().orElse(null),
        source.description().orElse(null),
        updatedAt == null ? null : updatedAt.toString());
  }
}
