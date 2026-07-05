package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;

/** Response view of a configured source. */
public record SourceResponse(
    String id,
    SourceType type,
    String uriOrPath,
    String ref,
    List<String> include,
    List<String> ignore,
    String name,
    String description) {

  static SourceResponse from(Source source) {
    return new SourceResponse(
        source.sourceId(),
        source.type(),
        source.uriOrPath(),
        source.ref().orElse(null),
        source.include(),
        source.ignore(),
        source.name().orElse(null),
        source.description().orElse(null));
  }
}
