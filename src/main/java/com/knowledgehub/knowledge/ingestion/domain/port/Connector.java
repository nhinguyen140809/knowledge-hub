package com.knowledgehub.knowledge.ingestion.domain.port;

import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.stream.Stream;

/**
 * Port that pulls raw artifacts from a source. Each {@link SourceType} has its own adapter (Git,
 * filesystem); the application picks the one whose {@link #supports(SourceType)} matches.
 */
public interface Connector {

  /** Whether this connector handles the given source type. */
  boolean supports(SourceType type);

  /**
   * Streams the source's artifacts (already filtered by include/ignore), reading content lazily.
   *
   * <p>The returned stream holds resources (a directory walk, an open repository) and <strong>must
   * be closed</strong> by the caller (try-with-resources). Files that cannot be read are skipped
   * and logged, never aborting the stream.
   *
   * @param source the source to fetch from
   * @return a lazily-evaluated, closeable stream of raw artifacts
   */
  Stream<RawArtifact> fetch(Source source);
}
