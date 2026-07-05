package com.knowledgehub.knowledge.ingestion.infrastructure.persistence;

import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Neo4j-backed {@link SourceRepository}. Delegates to a Spring Data repository and maps between the
 * persistence {@link SourceNode} and the domain {@link Source} at this boundary, so {@code @Node}
 * types never leak above infrastructure.
 */
@Component
class Neo4jSourceAdapter implements SourceRepository {

  private final SourceNodeRepository repository;

  Neo4jSourceAdapter(SourceNodeRepository repository) {
    this.repository = repository;
  }

  @Override
  public Source save(Source source) {
    repository.save(toNode(source));
    return source;
  }

  @Override
  public Optional<Source> findById(String sourceId) {
    return repository.findById(sourceId).map(Neo4jSourceAdapter::toDomain);
  }

  @Override
  public List<Source> findAll() {
    return repository.findAll().stream().map(Neo4jSourceAdapter::toDomain).toList();
  }

  @Override
  public void deleteById(String sourceId) {
    repository.deleteById(sourceId);
  }

  private static SourceNode toNode(Source source) {
    return new SourceNode(
        source.sourceId(),
        source.type().name(),
        source.uriOrPath(),
        source.ref().orElse(null),
        source.include(),
        source.ignore(),
        source.name().orElse(null),
        source.description().orElse(null));
  }

  private static Source toDomain(SourceNode node) {
    return new Source(
        node.sourceId(),
        SourceType.valueOf(node.type()),
        node.uriOrPath(),
        node.ref(),
        node.include(),
        node.ignore(),
        node.name(),
        node.description());
  }
}
