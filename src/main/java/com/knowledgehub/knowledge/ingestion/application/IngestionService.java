package com.knowledgehub.knowledge.ingestion.application;

import com.knowledgehub.knowledge.ingestion.domain.Connector;
import com.knowledgehub.knowledge.ingestion.domain.DocumentReader;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fetches a source's raw artifacts and attaches extracted text, ready for chunking. Picks the
 * {@link Connector} for the source type and, per artifact, the first {@link DocumentReader} that
 * supports its media type. An artifact whose text cannot be extracted is skipped and logged, never
 * aborting the run (NFR-6.1). Does not chunk or embed — that is the indexing phase.
 */
@Service
public class IngestionService {

  private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

  private final SourceRepository sources;
  private final List<Connector> connectors;
  private final List<DocumentReader> readers;

  public IngestionService(
      SourceRepository sources, List<Connector> connectors, List<DocumentReader> readers) {
    this.sources = sources;
    this.connectors = connectors;
    this.readers = readers;
  }

  /**
   * Streams the source's artifacts with extracted text attached. The returned stream is lazy and
   * <strong>must be closed</strong> by the caller (it holds connector resources); on close it logs
   * the read/skipped counts.
   *
   * @throws SourceNotFoundException if the source does not exist
   */
  public Stream<RawArtifact> ingest(String sourceId) {
    Source source =
        sources.findById(sourceId).orElseThrow(() -> new SourceNotFoundException(sourceId));
    Connector connector =
        connectors.stream()
            .filter(c -> c.supports(source.type()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No connector for source type " + source.type()));

    log.info("Ingesting source {} ({})", sourceId, source.type());
    AtomicInteger read = new AtomicInteger();
    AtomicInteger skipped = new AtomicInteger();
    return connector
        .fetch(source)
        .map(artifact -> extractText(artifact, read, skipped))
        .flatMap(Optional::stream)
        .onClose(
            () ->
                log.info(
                    "Ingested source {}: {} artifacts read, {} skipped",
                    sourceId,
                    read.get(),
                    skipped.get()));
  }

  private Optional<RawArtifact> extractText(
      RawArtifact artifact, AtomicInteger read, AtomicInteger skipped) {
    try {
      DocumentReader reader =
          readers.stream()
              .filter(r -> r.supports(artifact.mediaType()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "No reader for media type " + artifact.mediaType()));
      RawArtifact withText = artifact.withText(reader.extractText(artifact));
      read.incrementAndGet();
      return Optional.of(withText);
    } catch (RuntimeException e) {
      skipped.incrementAndGet();
      log.warn(
          "Skipping artifact {} in source {}: {}",
          artifact.path(),
          artifact.provenance().sourceId(),
          e.toString());
      return Optional.empty();
    }
  }
}
