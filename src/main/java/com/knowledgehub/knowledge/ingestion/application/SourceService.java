package com.knowledgehub.knowledge.ingestion.application;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the lifecycle of configured {@link Source}s (register / list / get / remove). On a
 * successful registration it publishes a {@link SourceRegistered} event so the indexing flow can
 * react without this service depending on it.
 */
@Service
public class SourceService {

  private static final Logger log = LoggerFactory.getLogger(SourceService.class);

  private final SourceRepository repository;
  private final ApplicationEventPublisher events;

  public SourceService(SourceRepository repository, ApplicationEventPublisher events) {
    this.repository = repository;
    this.events = events;
  }

  /**
   * Registers a new source and announces it.
   *
   * @throws DuplicateSourceException if a source with the same id already exists
   */
  @Transactional
  public Source register(SourceSpec spec) {
    if (repository.findById(spec.id()).isPresent()) {
      throw new DuplicateSourceException(spec.id());
    }
    Source saved = repository.save(spec.toSource());
    log.info("Registered source {} ({})", saved.sourceId(), saved.type());
    events.publishEvent(new SourceRegistered(saved.sourceId()));
    return saved;
  }

  /** All configured sources. */
  @Transactional(readOnly = true)
  public List<Source> list() {
    return repository.findAll();
  }

  /**
   * Returns the source with the given id.
   *
   * @throws SourceNotFoundException if no such source exists
   */
  @Transactional(readOnly = true)
  public Source get(String sourceId) {
    return repository.findById(sourceId).orElseThrow(() -> new SourceNotFoundException(sourceId));
  }

  /**
   * Removes the source with the given id.
   *
   * @throws SourceNotFoundException if no such source exists
   */
  @Transactional
  public void remove(String sourceId) {
    if (repository.findById(sourceId).isEmpty()) {
      throw new SourceNotFoundException(sourceId);
    }
    repository.deleteById(sourceId);
    log.info("Removed source {}", sourceId);
    events.publishEvent(new SourceDeleted(sourceId));
  }
}
